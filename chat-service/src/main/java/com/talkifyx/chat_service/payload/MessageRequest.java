package com.talkifyx.chat_service.payload;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO sent to message-service POST /api/messages.
 * Field names match MessageRequest in message-service exactly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private Long roomId;
    private String content;
    private String type;
    private String mediaUrl;
    private String replyToMessageId;   // ← correct field name (matches message-service)
    private String senderName;
    private String senderAvatar;
}
