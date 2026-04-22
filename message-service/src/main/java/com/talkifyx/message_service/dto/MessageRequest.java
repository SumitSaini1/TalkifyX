package com.talkifyx.message_service.dto;

import com.talkifyx.message_service.entity.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageRequest {

    @NotNull
    private Long roomId;
    private String content;

    @NotNull
    private MessageType type;

    private String mediaUrl;
    private String replyToMessageId;
}