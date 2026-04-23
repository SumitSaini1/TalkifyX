package com.talkifyx.notification_service.service;

import com.talkifyx.notification_service.dto.NotificationResponse;
import com.talkifyx.notification_service.dto.SendNotificationRequest;
import com.talkifyx.notification_service.entity.NotificationType;
import org.springframework.data.domain.Page;

public interface NotificationService {

    NotificationResponse send(SendNotificationRequest request);

    Page<NotificationResponse> getUserNotifications(Long userId, int page, int size);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(String notificationId);

    void markAllAsRead(Long userId);

    void delete(String notificationId);

    Page<NotificationResponse> filterByType(Long userId, NotificationType type, int page, int size);
}