package com.talkifyx.chat_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkifyx.chat_service.client.MessageServiceClient;
import com.talkifyx.chat_service.client.NotificationServiceClient;
import com.talkifyx.chat_service.client.RoomServiceClient;
import com.talkifyx.chat_service.handler.ChatWebSocketHandler;
import com.talkifyx.chat_service.payload.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


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

    // @MessageMapping("/chat.send")
    // public void sendMessage(@Payload ChatPayload payload,
    // @Header("X-User-Id") String userId) {
    // payload.setSenderId(Long.parseLong(userId));
    // Object saved = messageServiceClient.saveMessage(payload);
    // messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(),
    // saved);
    // }
    // @MessageMapping("/chat.send")
    // public void sendMessage(@Payload ChatPayload payload,
    // @Header("X-User-Id") String userId) {
    // payload.setSenderId(Long.parseLong(userId));
    // Object saved =
    // messageServiceClient.saveMessage(payload,payload.getSenderId());
    // if (saved == null) {
    // System.err.println("[CHAT] saveMessage returned null — fallback triggered!");
    // return;
    // }
    // messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(),
    // saved);
    // }
    // @MessageMapping("/chat.send")
    // public void sendMessage(@Payload ChatPayload payload,
    // @Header("X-User-Id") String userId) {
    // payload.setSenderId(Long.parseLong(userId));
    // payload.setType("TEXT"); // force correct enum value
    // System.out.println("[CHAT] senderName=" + payload.getSenderName() + "
    // senderAvatar=" + payload.getSenderAvatar());
    // Object saved = messageServiceClient.saveMessage(payload,
    // payload.getSenderId());
    // if (saved == null) {
    // System.err.println("[CHAT] saveMessage returned null — fallback triggered!");
    // return;
    // }
    // messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(),
    // saved);
    // }
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
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), saved);

        // ← add this
        try {
            List<Map<String, Object>> members = roomServiceClient.getRoomMembers(payload.getRoomId());
            if (members != null) {
                for (Map<String, Object> member : members) {
                    Long memberId = Long.valueOf(member.get("userId").toString());
                    if (!memberId.equals(Long.parseLong(userId))) {
                        notificationServiceClient.sendNotification(Map.of(
                                "recipientId", memberId,
                                "actorId", Long.parseLong(userId),
                                "type", "NEW_MESSAGE",
                                "roomId", payload.getRoomId(),
                                "title", payload.getSenderName() != null ? payload.getSenderName() : "New Message",
                                "message", payload.getContent() != null ? payload.getContent() : "📎 Attachment"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CHAT] Notification failed: " + e.getMessage());
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
        ReadReceiptPayload receipt = new ReadReceiptPayload();
        receipt.setReaderId(Long.parseLong(userId));
        receipt.setRoomId(payload.getRoomId());
        receipt.setUpToMessageId(payload.getMessageId()); // map messageId → upToMessageId
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), receipt);
    }
}