package com.talkifyx.chat_service.payload;

import lombok.Data;

@Data
public class ReactionPayload {
    private Long senderId;
    private Long roomId;
    private String messageId;
    private String emoji;
}