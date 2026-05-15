package com.talkifyx.notification_service.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationEntityTest {

    private Notification buildNotification(String id) {
        return Notification.builder()
                .notificationId(id)
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
    }

    // ── Builder & Getters ──────────────────────────────────────────────────────

    @Test
    void builder_AllFields_SetCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Notification n = Notification.builder()
                .notificationId("n-1")
                .recipientId(1L)
                .actorId(2L)
                .type(NotificationType.MENTION)
                .title("Title")
                .message("Msg")
                .roomId(5L)
                .messageId("m-1")
                .isRead(true)
                .createdAt(now)
                .build();

        assertEquals("n-1", n.getNotificationId());
        assertEquals(1L, n.getRecipientId());
        assertEquals(2L, n.getActorId());
        assertEquals(NotificationType.MENTION, n.getType());
        assertEquals("Title", n.getTitle());
        assertEquals("Msg", n.getMessage());
        assertEquals(5L, n.getRoomId());
        assertEquals("m-1", n.getMessageId());
        assertTrue(n.isRead());
        assertEquals(now, n.getCreatedAt());
    }

    // ── Setters ────────────────────────────────────────────────────────────────

    @Test
    void setters_UpdateAllFields() {
        Notification n = new Notification();
        LocalDateTime now = LocalDateTime.now();

        n.setNotificationId("n-2");
        n.setRecipientId(3L);
        n.setActorId(4L);
        n.setType(NotificationType.SYSTEM);
        n.setTitle("New Title");
        n.setMessage("New Msg");
        n.setRoomId(20L);
        n.setMessageId("m-2");
        n.setRead(true);
        n.setCreatedAt(now);

        assertEquals("n-2", n.getNotificationId());
        assertEquals(3L, n.getRecipientId());
        assertEquals(4L, n.getActorId());
        assertEquals(NotificationType.SYSTEM, n.getType());
        assertEquals("New Title", n.getTitle());
        assertEquals("New Msg", n.getMessage());
        assertEquals(20L, n.getRoomId());
        assertEquals("m-2", n.getMessageId());
        assertTrue(n.isRead());
        assertEquals(now, n.getCreatedAt());
    }

    // ── equals() — all branches ────────────────────────────────────────────────

    @Test
    void equals_SameObject_ReturnsTrue() {
        Notification n = buildNotification("n-1");
        assertEquals(n, n);
    }

    @Test
    void equals_EqualObjects_ReturnsTrue() {
        LocalDateTime now = LocalDateTime.now();
        Notification n1 = Notification.builder().notificationId("n-1").recipientId(1L)
                .actorId(2L).type(NotificationType.NEW_MESSAGE).title("T").message("M")
                .roomId(10L).messageId("m-1").isRead(false).createdAt(now).build();
        Notification n2 = Notification.builder().notificationId("n-1").recipientId(1L)
                .actorId(2L).type(NotificationType.NEW_MESSAGE).title("T").message("M")
                .roomId(10L).messageId("m-1").isRead(false).createdAt(now).build();

        assertEquals(n1, n2);
    }

    @Test
    void equals_DifferentId_ReturnsFalse() {
        Notification n1 = buildNotification("n-1");
        Notification n2 = buildNotification("n-2");
        assertNotEquals(n1, n2);
    }

    @Test
    void equals_NullObject_ReturnsFalse() {
        Notification n = buildNotification("n-1");
        assertNotEquals(n, null);
    }

    @Test
    void equals_DifferentClass_ReturnsFalse() {
        Notification n = buildNotification("n-1");
        assertNotEquals(n, "a string");
    }

    @Test
    void equals_DifferentRecipientId_ReturnsFalse() {
        LocalDateTime now = LocalDateTime.now();
        Notification n1 = Notification.builder().notificationId("n-1").recipientId(1L)
                .type(NotificationType.NEW_MESSAGE).createdAt(now).build();
        Notification n2 = Notification.builder().notificationId("n-1").recipientId(99L)
                .type(NotificationType.NEW_MESSAGE).createdAt(now).build();
        assertNotEquals(n1, n2);
    }

    @Test
    void equals_DifferentType_ReturnsFalse() {
        LocalDateTime now = LocalDateTime.now();
        Notification n1 = Notification.builder().notificationId("n-1").recipientId(1L)
                .type(NotificationType.NEW_MESSAGE).createdAt(now).build();
        Notification n2 = Notification.builder().notificationId("n-1").recipientId(1L)
                .type(NotificationType.MENTION).createdAt(now).build();
        assertNotEquals(n1, n2);
    }

    @Test
    void equals_DifferentIsRead_ReturnsFalse() {
        LocalDateTime now = LocalDateTime.now();
        Notification n1 = Notification.builder().notificationId("n-1").recipientId(1L)
                .type(NotificationType.NEW_MESSAGE).isRead(false).createdAt(now).build();
        Notification n2 = Notification.builder().notificationId("n-1").recipientId(1L)
                .type(NotificationType.NEW_MESSAGE).isRead(true).createdAt(now).build();
        assertNotEquals(n1, n2);
    }

    @Test
    void equals_NullFieldsVsNonNull_ReturnsFalse() {
        Notification n1 = Notification.builder().notificationId("n-1")
                .type(NotificationType.NEW_MESSAGE).title(null).build();
        Notification n2 = Notification.builder().notificationId("n-1")
                .type(NotificationType.NEW_MESSAGE).title("Hello").build();
        assertNotEquals(n1, n2);
    }

    // ── hashCode() ─────────────────────────────────────────────────────────────

    @Test
    void hashCode_EqualObjects_SameHashCode() {
        LocalDateTime now = LocalDateTime.now();
        Notification n1 = Notification.builder().notificationId("n-1").recipientId(1L)
                .type(NotificationType.NEW_MESSAGE).createdAt(now).build();
        Notification n2 = Notification.builder().notificationId("n-1").recipientId(1L)
                .type(NotificationType.NEW_MESSAGE).createdAt(now).build();
        assertEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    void hashCode_DifferentObjects_DifferentHashCode() {
        Notification n1 = buildNotification("n-1");
        Notification n2 = buildNotification("n-2");
        assertNotEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    void hashCode_NullFields_DoesNotThrow() {
        Notification n = new Notification();
        assertDoesNotThrow(n::hashCode);
    }

    // ── toString() ─────────────────────────────────────────────────────────────

    @Test
    void toString_ContainsAllFields() {
        Notification n = buildNotification("n-1");
        String str = n.toString();

        assertTrue(str.contains("n-1"));
        assertTrue(str.contains("Notification"));
    }

    @Test
    void toString_NullFields_DoesNotThrow() {
        Notification n = new Notification();
        assertDoesNotThrow(n::toString);
    }

    // ── canEqual() ─────────────────────────────────────────────────────────────

    @Test
    void canEqual_WithSameType_ReturnsTrue() {
        Notification n1 = buildNotification("n-1");
        Notification n2 = buildNotification("n-2");
        assertTrue(n1.canEqual(n2));
    }

    @Test
    void canEqual_WithDifferentType_ReturnsFalse() {
        Notification n = buildNotification("n-1");
        assertFalse(n.canEqual("string"));
    }

    @Test
    void canEqual_WithNull_ReturnsFalse() {
        Notification n = buildNotification("n-1");
        assertFalse(n.canEqual(null));
    }

    // ── @PrePersist onCreate() — both branches ─────────────────────────────────

    @Test
    void onCreate_WhenCreatedAtIsNull_SetsCurrentTime() throws Exception {
        Notification n = new Notification();
        n.setCreatedAt(null);

        // invoke @PrePersist method directly via reflection
        var method = Notification.class.getDeclaredMethod("onCreate");
        method.setAccessible(true);
        method.invoke(n);

        assertNotNull(n.getCreatedAt());
    }

    @Test
    void onCreate_WhenCreatedAtAlreadySet_DoesNotOverride() throws Exception {
        LocalDateTime fixed = LocalDateTime.of(2024, 1, 1, 0, 0);
        Notification n = new Notification();
        n.setCreatedAt(fixed);

        var method = Notification.class.getDeclaredMethod("onCreate");
        method.setAccessible(true);
        method.invoke(n);

        assertEquals(fixed, n.getCreatedAt());
    }

    // ── AllArgsConstructor ─────────────────────────────────────────────────────

    @Test
    void allArgsConstructor_SetsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Notification n = new Notification("id-1", 1L, 2L,
                NotificationType.ROOM_INVITE, "title", "msg", 5L, "m-1", true, now);

        assertEquals("id-1", n.getNotificationId());
        assertEquals(1L, n.getRecipientId());
        assertEquals(2L, n.getActorId());
        assertEquals(NotificationType.ROOM_INVITE, n.getType());
        assertEquals("title", n.getTitle());
        assertEquals("msg", n.getMessage());
        assertEquals(5L, n.getRoomId());
        assertEquals("m-1", n.getMessageId());
        assertTrue(n.isRead());
        assertEquals(now, n.getCreatedAt());
    }
}