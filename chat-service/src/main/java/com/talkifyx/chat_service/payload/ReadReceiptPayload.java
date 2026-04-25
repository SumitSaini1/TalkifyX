package com.talkifyx.chat_service.payload;

import lombok.Data;

@Data
public class ReadReceiptPayload {
    private Long readerId;
    private Long roomId;
    private String upToMessageId;
}