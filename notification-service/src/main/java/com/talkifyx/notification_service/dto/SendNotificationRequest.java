package com.talkifyx.notification_service.dto;

import com.talkifyx.notification_service.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotNull
    private Long recipientId;

    private Long actorId;

    @NotNull
    private NotificationType type;

    private String title;
    private String message;
    private Long roomId;
    private String messageId;
}