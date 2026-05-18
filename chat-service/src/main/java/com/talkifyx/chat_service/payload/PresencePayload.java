package com.talkifyx.chat_service.payload;

import lombok.Data;

@Data
public class PresencePayload {
    private Long userId;
    private String status;
    private String customMessage;
}