package com.talkifyx.chat_service.payload;

import lombok.Data;

@Data
public class TypingPayload {
    private Long senderId;
    private Long roomId;
    private boolean isTyping;
}