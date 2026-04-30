package com.talkifyx.chat_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkifyx.chat_service.client.MessageServiceClient;
import com.talkifyx.chat_service.client.NotificationServiceClient;
import com.talkifyx.chat_service.client.PresenceServiceClient;
import com.talkifyx.chat_service.client.RoomServiceClient;
import com.talkifyx.chat_service.handler.ChatWebSocketHandler;
import com.talkifyx.chat_service.payload.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Tag(name = "Chat STOMP", description = "STOMP WebSocket endpoints")
public class ChatStompController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final MessageServiceClient messageServiceClient;
    private final ObjectMapper objectMapper;
    private final RoomServiceClient roomServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final PresenceServiceClient presenceServiceClient;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload String rawBody,
            @Header("X-User-Id") String userId) throws Exception {
        ChatPayload payload = objectMapper.readValue(rawBody, ChatPayload.class);
        payload.setSenderId(Long.parseLong(userId));
        payload.setType("TEXT");
        System.out.println("[CHAT] senderName=" + payload.getSenderName());

        Object saved = messageServiceClient.saveMessage(payload, payload.getSenderId());
        if (saved == null) {
            System.err.println("[CHAT] saveMessage returned null — fallback triggered!");
            return;
        }

        // Broadcast message to room topic
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), saved);

        // Persist room.lastMessageAt and notify all members' personal topics (ROOM_UPDATED)
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        try {
            List<Map<String, Object>> members = roomServiceClient.getRoomMembers(payload.getRoomId());
            // Update lastMessageAt in room-service DB
            roomServiceClient.updateLastMessageAt(payload.getRoomId(), nowStr);

            // Broadcast ROOM_UPDATED to each member's personal user topic
            if (members != null) {
                Map<String, Object> roomUpdatedEvent = Map.of(
                        "eventType", "ROOM_UPDATED",
                        "roomId", payload.getRoomId(),
                        "lastMessageAt", nowStr,
                        "lastMessage", saved
                );
                for (Map<String, Object> member : members) {
                    Long memberId = Long.valueOf(member.get("userId").toString());
                    messagingTemplate.convertAndSend("/topic/user/" + memberId, roomUpdatedEvent);
                }

                // Send push notifications ONLY to offline non-sender members
                for (Map<String, Object> member : members) {
                    Long memberId = Long.valueOf(member.get("userId").toString());
                    if (!memberId.equals(Long.parseLong(userId))) {
                        try {
                            // Check presence — skip notification if user is ONLINE
                            Object presenceRaw = presenceServiceClient.getPresence(memberId);
                            String status = extractStatus(presenceRaw);
                            if ("ONLINE".equalsIgnoreCase(status)) {
                                continue; // User is online — WS message is enough
                            }
                            notificationServiceClient.sendNotification(Map.of(
                                    "recipientId", memberId,
                                    "actorId", Long.parseLong(userId),
                                    "type", "NEW_MESSAGE",
                                    "roomId", payload.getRoomId(),
                                    "title", payload.getSenderName() != null ? payload.getSenderName() : "New Message",
                                    "message", payload.getContent() != null ? payload.getContent() : "📎 Attachment"));
                        } catch (Exception e) {
                            System.err.println("[CHAT] Notification failed: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CHAT] Room update/notify failed: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.typing")
    @Operation(summary = "Typing indicator")
    public void typing(@Payload TypingPayload payload,
            @Header("X-User-Id") String userId) {
        payload.setSenderId(Long.parseLong(userId));
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
    }

    @MessageMapping("/chat.read")
    public void readReceipt(@Payload ChatPayload payload,
            @Header("X-User-Id") String userId) {
        Long readerId = Long.parseLong(userId);

        // 1) Build and broadcast WebSocket read receipt event to room
        ReadReceiptPayload receipt = new ReadReceiptPayload();
        receipt.setReaderId(readerId);
        receipt.setRoomId(payload.getRoomId());
        receipt.setUpToMessageId(payload.getMessageId());
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), receipt);

        // 2) Persist lastReadAt in room-service DB (fire-and-forget)
        try {
            roomServiceClient.updateLastRead(payload.getRoomId(), readerId);
        } catch (Exception e) {
            System.err.println("[CHAT] updateLastRead failed: " + e.getMessage());
        }

        // 3) Bulk-mark all messages in room as READ in message-service DB (fire-and-forget)
        try {
            messageServiceClient.markRoomRead(payload.getRoomId(), readerId);
        } catch (Exception e) {
            System.err.println("[CHAT] markRoomRead failed: " + e.getMessage());
        }
    }

    /**
     * Safely extracts the "status" field from whatever presence-service returns.
     * Handles both Map responses and plain String responses.
     */
    @SuppressWarnings("unchecked")
    private String extractStatus(Object presenceRaw) {
        if (presenceRaw == null) return "OFFLINE";
        if (presenceRaw instanceof Map) {
            Object status = ((Map<String, Object>) presenceRaw).get("status");
            return status != null ? status.toString() : "OFFLINE";
        }
        return presenceRaw.toString();
    }
}