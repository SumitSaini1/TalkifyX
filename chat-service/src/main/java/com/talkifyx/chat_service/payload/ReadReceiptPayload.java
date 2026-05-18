package com.talkifyx.chat_service.payload;

import lombok.Data;

@Data
public class ReadReceiptPayload {
    private String type = "READ_RECEIPT";
    private Long readerId;
    private Long roomId;
    private String upToMessageId;
}