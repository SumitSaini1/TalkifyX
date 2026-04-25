package com.talkifyx.notification_service.dto;

import com.talkifyx.notification_service.entity.NotificationType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private String notificationId;
    private Long recipientId;
    private Long actorId;
    private NotificationType type;
    private String title;
    private String message;
    private Long roomId;
    private String messageId;
    private boolean isRead;
    private LocalDateTime createdAt;
}