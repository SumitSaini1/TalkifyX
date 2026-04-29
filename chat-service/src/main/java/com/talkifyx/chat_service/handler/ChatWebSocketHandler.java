package com.talkifyx.chat_service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkifyx.chat_service.client.*;
import com.talkifyx.chat_service.payload.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageServiceClient messageServiceClient;
    private final PresenceServiceClient presenceServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final RoomServiceClient roomServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Long> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        sessions.put(session.getId(), userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());

        PresencePayload presence = new PresencePayload();
        presence.setUserId(userId);
        presence.setStatus("ONLINE");
        presenceServiceClient.updatePresence(userId, presence);
        broadcastPresence(userId, "ONLINE");
        log.info("WS connected: userId={} sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = sessions.get(session.getId());
        if (userId == null)
            return;

        ChatPayload payload = objectMapper.readValue(message.getPayload(), ChatPayload.class);
        switch (payload.getType()) {
            case "CHAT_MESSAGE" -> handleChatMessage(payload, userId);
            case "TYPING_INDICATOR" -> handleTyping(payload, userId);
            case "READ_RECEIPT" -> handleReadReceipt(payload, userId);
            case "REACTION" -> handleReaction(payload, userId);
            case "MESSAGE_EDIT" -> handleMessageEdit(payload, userId);
            case "MESSAGE_DELETE" -> handleMessageDelete(payload, userId);
            default -> log.warn("Unknown type: {}", payload.getType());
        }
    }

    private void handleChatMessage(ChatPayload payload, Long userId) {
        Object saved = messageServiceClient.saveMessage(payload, userId);
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), saved);

        try {
            List<Map<String, Object>> members = roomServiceClient.getRoomMembers(payload.getRoomId());
            log.info("Members fetched size: {}", members != null ? members.size() : "NULL"); // ← add here
            if (members == null)
                return;
            for (Map<String, Object> member : members) {
                Long memberId = Long.valueOf(member.get("userId").toString());
                log.info("Notifying memberId={}", memberId);
                if (!memberId.equals(userId)) {
                    notificationServiceClient.sendNotification(Map.of(
                            "recipientId", memberId,
                            "actorId", userId,
                            "type", "NEW_MESSAGE",
                            "roomId", payload.getRoomId(),
                            "title", "New Message",
                            "message", payload.getContent() != null ? payload.getContent() : "📎 Attachment"));
                }
            }
        } catch (Exception e) {
            log.warn("Notification send failed: {}", e.getMessage());
        }
    }

    private void handleTyping(ChatPayload payload, Long userId) {
        TypingPayload typing = new TypingPayload();
        typing.setSenderId(userId);
        typing.setRoomId(payload.getRoomId());
        typing.setTyping(true);
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), typing);
    }

    private void handleReadReceipt(ChatPayload payload, Long userId) {
        ReadReceiptPayload receipt = new ReadReceiptPayload();
        receipt.setReaderId(userId);
        receipt.setRoomId(payload.getRoomId());
        receipt.setUpToMessageId(payload.getMessageId());
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), receipt);
    }

    private void handleReaction(ChatPayload payload, Long userId) {
        payload.setSenderId(userId);
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
    }

    private void handleMessageEdit(ChatPayload payload, Long userId) {
        messageServiceClient.editMessage(payload.getMessageId(), payload.getNewContent());
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
    }

    private void handleMessageDelete(ChatPayload payload, Long userId) {
        messageServiceClient.deleteMessage(payload.getDeletedId());
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessions.remove(session.getId());
        if (userId != null) {
            Set<String> set = userSessions.get(userId);
            if (set != null) {
                set.remove(session.getId());
                if (set.isEmpty()) {
                    userSessions.remove(userId);
                    PresencePayload presence = new PresencePayload();
                    presence.setUserId(userId);
                    presence.setStatus("OFFLINE");
                    presenceServiceClient.updatePresence(userId, presence);
                    broadcastPresence(userId, "OFFLINE");
                }
            }
        }
        log.info("WS closed: sessionId={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable ex) {
        log.error("WS error: sessionId={} error={}", session.getId(), ex.getMessage());
    }

    public void broadcastPresence(Long userId, String status) {
        PresencePayload p = new PresencePayload();
        p.setUserId(userId);
        p.setStatus(status);
        messagingTemplate.convertAndSend("/topic/presence", p);
    }

    public void sendToUser(Long userId, Object payload) {
        messagingTemplate.convertAndSend("/topic/user/" + userId, payload);
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        List<String> headers = session.getHandshakeHeaders().get("X-User-Id");
        if (headers != null && !headers.isEmpty()) {
            try {
                return Long.parseLong(headers.get(0));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}