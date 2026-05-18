package com.talkifyx.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.talkifyx.notification_service.entity.NotificationType;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdAt;
}