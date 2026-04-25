package com.talkifyx.presence_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceRequest {
    private Long userId;
    private String status;
    private String customMessage;
    private String deviceType;
    private String ipAddress;
    private String sessionId;
}