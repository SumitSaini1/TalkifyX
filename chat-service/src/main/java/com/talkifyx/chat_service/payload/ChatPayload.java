package com.talkifyx.chat_service.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ChatPayload {
    private String type;
    private Long senderId;
    private Long roomId;
    private String content;
    private String replyToId;
    private String messageId;
    private String newContent;
    private String deletedId;
    private String emoji;
    @JsonProperty("senderName")
    private String senderName;

    @JsonProperty("senderAvatar")
    private String senderAvatar;

    private String deleteType;
}