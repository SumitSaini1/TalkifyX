package com.talkifyx.notification_service.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.talkifyx.notification_service.dto.NotificationResponse;
import com.talkifyx.notification_service.dto.PresenceDto;
import com.talkifyx.notification_service.dto.SendNotificationRequest;
import com.talkifyx.notification_service.dto.UserProfileDto;
import com.talkifyx.notification_service.entity.Notification;
import com.talkifyx.notification_service.entity.NotificationType;
import com.talkifyx.notification_service.feign.AuthServiceClient;
import com.talkifyx.notification_service.feign.PresenceServiceClient;
import com.talkifyx.notification_service.repository.NotificationRepository;
import com.talkifyx.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final PresenceServiceClient presenceServiceClient;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .notificationId(UUID.randomUUID().toString())
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .roomId(request.getRoomId())
                .messageId(request.getMessageId())
                .isRead(false)
                .build();

        notificationRepository.save(notification);

        try {
            PresenceDto presence = presenceServiceClient.getPresence(request.getRecipientId());
            if ("OFFLINE".equalsIgnoreCase(presence.getStatus())) {
                UserProfileDto profile = authServiceClient.getUserProfile(
                        String.valueOf(request.getRecipientId()));
                if (profile != null && profile.getFcmToken() != null) {
                    sendFcmPush(profile.getFcmToken(), request.getTitle(), request.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("FCM push failed for recipientId={}: {}", request.getRecipientId(), e.getMessage());
        }

        return toResponse(notification);
    }

    private void sendFcmPush(String fcmToken, String title, String body) {
        try {
            Message msg = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            String response = FirebaseMessaging.getInstance().send(msg);
            log.info("FCM sent: {}", response);
        } catch (Exception e) {
            log.error("FCM send error: {}", e.getMessage());
        }
    }

    @Override
    public Page<NotificationResponse> getUserNotifications(Long userId, int page, int size) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsRead(userId, false);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        n.setRead(true);
        return toResponse(notificationRepository.save(n));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllReadByRecipientId(userId);
    }

    @Override
    @Transactional
    public void delete(String notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public Page<NotificationResponse> filterByType(Long userId, NotificationType type, int page, int size) {
        return notificationRepository
                .findByRecipientIdAndTypeOrderByCreatedAtDesc(userId, type, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setNotificationId(n.getNotificationId());
        r.setRecipientId(n.getRecipientId());
        r.setActorId(n.getActorId());
        r.setType(n.getType());
        r.setTitle(n.getTitle());
        r.setMessage(n.getMessage());
        r.setRoomId(n.getRoomId());
        r.setMessageId(n.getMessageId());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }
}