package com.talkifyx.notification_service.dto;

import lombok.Data;

@Data
public class PresenceDto {
    private Long userId;
    private String status;
    private String sessionId;
}