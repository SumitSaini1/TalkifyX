package com.talkifyx.presence_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceResponse {
    private Long presenceId;
    private Long userId;
    private String status;
    private String customMessage;
    private String deviceType;
    private String sessionId;
    private LocalDateTime connectedAt;
    private LocalDateTime lastPingAt;
}