package com.talkifyx.notification_service.service;

import com.talkifyx.notification_service.dto.*;
import com.talkifyx.notification_service.entity.Notification;
import com.talkifyx.notification_service.entity.NotificationType;
import com.talkifyx.notification_service.exception.ResourceNotFoundException;
import com.talkifyx.notification_service.feign.AuthServiceClient;
import com.talkifyx.notification_service.feign.PresenceServiceClient;
import com.talkifyx.notification_service.repository.NotificationRepository;
import com.talkifyx.notification_service.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PresenceServiceClient presenceServiceClient;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private SendNotificationRequest request;
    private Notification notification;

    @BeforeEach
    void setUp() {
        request = new SendNotificationRequest();
        request.setRecipientId(1L);
        request.setActorId(2L);
        request.setType(NotificationType.NEW_MESSAGE);
        request.setTitle("Hello");
        request.setMessage("You have a message");
        request.setRoomId(10L);
        request.setMessageId("msg-1");

        notification = Notification.builder()
                .notificationId("notif-1")
                .recipientId(1L)
                .actorId(2L)
                .type(NotificationType.NEW_MESSAGE)
                .title("Hello")
                .message("You have a message")
                .roomId(10L)
                .messageId("msg-1")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── send() ───────────────────────────────────────────────────────────────

    @Test
    void send_UserOfflineWithFcmToken_SavesAndReturnResponse() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        PresenceDto presence = new PresenceDto();
        presence.setStatus("OFFLINE");
        when(presenceServiceClient.getPresence(1L)).thenReturn(presence);

        UserProfileDto profile = new UserProfileDto();
        profile.setFcmToken("fcm-token-123");
        when(authServiceClient.getUserProfile("1")).thenReturn(profile);

        NotificationResponse response = notificationService.send(request);

        assertNotNull(response);
        assertEquals("Hello", response.getTitle());
        assertEquals(1L, response.getRecipientId());
        assertFalse(response.isRead());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void send_UserOnline_SkipsFcmPush() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        PresenceDto presence = new PresenceDto();
        presence.setStatus("ONLINE");
        when(presenceServiceClient.getPresence(1L)).thenReturn(presence);

        NotificationResponse response = notificationService.send(request);

        assertNotNull(response);
        verify(authServiceClient, never()).getUserProfile(any());
    }

    @Test
    void send_UserOfflineNoFcmToken_SkipsFcmPush() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        PresenceDto presence = new PresenceDto();
        presence.setStatus("OFFLINE");
        when(presenceServiceClient.getPresence(1L)).thenReturn(presence);

        UserProfileDto profile = new UserProfileDto();
        profile.setFcmToken(null);
        when(authServiceClient.getUserProfile("1")).thenReturn(profile);

        NotificationResponse response = notificationService.send(request);

        assertNotNull(response);
        assertEquals("You have a message", response.getMessage());
    }

    @Test
    void send_UserOfflineNullProfile_SkipsFcmPush() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        PresenceDto presence = new PresenceDto();
        presence.setStatus("OFFLINE");
        when(presenceServiceClient.getPresence(1L)).thenReturn(presence);

        when(authServiceClient.getUserProfile("1")).thenReturn(null);

        NotificationResponse response = notificationService.send(request);

        assertNotNull(response);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void send_PresenceServiceThrowsException_StillReturnsResponse() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(presenceServiceClient.getPresence(1L)).thenThrow(new RuntimeException("Service down"));

        NotificationResponse response = notificationService.send(request);

        assertNotNull(response);
        assertEquals("Hello", response.getTitle());
    }

    @Test
    void send_AllFieldsMappedCorrectly() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        PresenceDto presence = new PresenceDto();
        presence.setStatus("ONLINE");
        when(presenceServiceClient.getPresence(1L)).thenReturn(presence);

        NotificationResponse response = notificationService.send(request);

        assertEquals(NotificationType.NEW_MESSAGE, response.getType());
        assertEquals(2L, response.getActorId());
        assertEquals(10L, response.getRoomId());
        assertEquals("msg-1", response.getMessageId());
    }

    // ─── getUserNotifications() ────────────────────────────────────────────────

    @Test
    void getUserNotifications_ReturnsPagedResponse() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUserNotifications(1L, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Hello", result.getContent().get(0).getTitle());
    }

    @Test
    void getUserNotifications_EmptyList_ReturnsEmptyPage() {
        Page<Notification> emptyPage = new PageImpl<>(Collections.emptyList());
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<NotificationResponse> result = notificationService.getUserNotifications(1L, 0, 10);

        assertEquals(0, result.getTotalElements());
    }

    // ─── getUnreadCount() ─────────────────────────────────────────────────────

    @Test
    void getUnreadCount_ReturnsCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    void getUnreadCount_ReturnsZero_WhenAllRead() {
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(0L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(0L, count);
    }

    // ─── markAsRead() ─────────────────────────────────────────────────────────

    @Test
    void markAsRead_ValidId_MarksAndReturns() {
        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notification));
        notification.setRead(true);
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead("notif-1");

        assertTrue(response.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_NotFound_ThrowsResourceNotFoundException() {
        when(notificationRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead("bad-id"));
    }

    // ─── markAllAsRead() ──────────────────────────────────────────────────────

    @Test
    void markAllAsRead_CallsRepository() {
        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllReadByRecipientId(1L);
    }

    // ─── delete() ─────────────────────────────────────────────────────────────

    @Test
    void delete_ValidId_DeletesSuccessfully() {
        when(notificationRepository.existsById("notif-1")).thenReturn(true);

        notificationService.delete("notif-1");

        verify(notificationRepository).deleteById("notif-1");
    }

    @Test
    void delete_NotFound_ThrowsResourceNotFoundException() {
        when(notificationRepository.existsById("bad-id")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.delete("bad-id"));
    }

    // ─── filterByType() ───────────────────────────────────────────────────────

    @Test
    void filterByType_ReturnsFilteredPage() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(
                eq(1L), eq(NotificationType.NEW_MESSAGE), any(Pageable.class)))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.filterByType(
                1L, NotificationType.NEW_MESSAGE, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(NotificationType.NEW_MESSAGE, result.getContent().get(0).getType());
    }

    @Test
    void filterByType_EmptyResult_ReturnsEmptyPage() {
        Page<Notification> emptyPage = new PageImpl<>(Collections.emptyList());
        when(notificationRepository.findByRecipientIdAndTypeOrderByCreatedAtDesc(
                eq(1L), eq(NotificationType.MENTION), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<NotificationResponse> result = notificationService.filterByType(
                1L, NotificationType.MENTION, 0, 10);

        assertEquals(0, result.getTotalElements());
    }
}