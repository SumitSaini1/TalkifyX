package com.talkifyx.notification_service.dto;

import com.talkifyx.notification_service.entity.Notification;
import com.talkifyx.notification_service.entity.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void userProfileDto_GettersSetters_Work() {
        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(1L);
        dto.setUsername("john");
        dto.setEmail("john@example.com");
        dto.setFcmToken("token-abc");

        assertEquals(1L, dto.getUserId());
        assertEquals("john", dto.getUsername());
        assertEquals("john@example.com", dto.getEmail());
        assertEquals("token-abc", dto.getFcmToken());
    }

    // ── PresenceDto ───────────────────────────────────────────────────────────

    @Test
    void presenceDto_GettersSetters_Work() {
        PresenceDto dto = new PresenceDto();
        dto.setUserId(2L);
        dto.setStatus("ONLINE");
        dto.setSessionId("session-xyz");

        assertEquals(2L, dto.getUserId());
        assertEquals("ONLINE", dto.getStatus());
        assertEquals("session-xyz", dto.getSessionId());
    }

    // ── SendNotificationRequest ───────────────────────────────────────────────

    @Test
    void sendNotificationRequest_GettersSetters_Work() {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipientId(1L);
        req.setActorId(2L);
        req.setType(NotificationType.MENTION);
        req.setTitle("Test");
        req.setMessage("Test message");
        req.setRoomId(10L);
        req.setMessageId("msg-99");

        assertEquals(1L, req.getRecipientId());
        assertEquals(2L, req.getActorId());
        assertEquals(NotificationType.MENTION, req.getType());
        assertEquals("Test", req.getTitle());
        assertEquals("Test message", req.getMessage());
        assertEquals(10L, req.getRoomId());
        assertEquals("msg-99", req.getMessageId());
    }

    // ── NotificationResponse ──────────────────────────────────────────────────

    @Test
    void notificationResponse_GettersSetters_Work() {
        NotificationResponse res = new NotificationResponse();
        LocalDateTime now = LocalDateTime.now();
        res.setNotificationId("n-1");
        res.setRecipientId(1L);
        res.setActorId(2L);
        res.setType(NotificationType.SYSTEM);
        res.setTitle("Hi");
        res.setMessage("Msg");
        res.setRoomId(5L);
        res.setMessageId("m-1");
        res.setRead(true);
        res.setCreatedAt(now);

        assertEquals("n-1", res.getNotificationId());
        assertEquals(1L, res.getRecipientId());
        assertEquals(NotificationType.SYSTEM, res.getType());
        assertTrue(res.isRead());
        assertEquals(now, res.getCreatedAt());
    }

    // ── Notification Entity ───────────────────────────────────────────────────

    @Test
    void notification_Builder_Works() {
        Notification n = Notification.builder()
                .notificationId("n-1")
                .recipientId(1L)
                .actorId(2L)
                .type(NotificationType.NEW_MESSAGE)
                .title("Hello")
                .message("World")
                .roomId(10L)
                .messageId("msg-1")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        assertEquals("n-1", n.getNotificationId());
        assertEquals(NotificationType.NEW_MESSAGE, n.getType());
        assertFalse(n.isRead());
    }

    @Test
    void notification_SetRead_UpdatesValue() {
        Notification n = new Notification();
        n.setRead(true);
        assertTrue(n.isRead());
    }

    @Test
    void notificationType_AllValues_Exist() {
        assertEquals(4, NotificationType.values().length);
        assertNotNull(NotificationType.valueOf("NEW_MESSAGE"));
        assertNotNull(NotificationType.valueOf("MENTION"));
        assertNotNull(NotificationType.valueOf("ROOM_INVITE"));
        assertNotNull(NotificationType.valueOf("SYSTEM"));
    }

    @Test
    void notification_NoArgsConstructor_Works() {
        Notification n = new Notification();
        assertNotNull(n);
        assertFalse(n.isRead());
    }

    @Test
    void notification_AllArgsConstructor_Works() {
        LocalDateTime now = LocalDateTime.now();
        Notification n = new Notification("id-1", 1L, 2L,
                NotificationType.MENTION, "title", "msg", 5L, "m-1", false, now);

        assertEquals("id-1", n.getNotificationId());
        assertEquals(1L, n.getRecipientId());
        assertEquals(now, n.getCreatedAt());
    }
}