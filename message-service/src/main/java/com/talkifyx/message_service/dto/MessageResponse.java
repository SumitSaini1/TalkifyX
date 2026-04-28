package com.talkifyx.message_service.dto;

import com.talkifyx.message_service.entity.DeliveryStatus;
import com.talkifyx.message_service.entity.MessageType;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String messageId;
    private Long roomId;
    private Long senderId;
    private String content;
    private MessageType type;
    private String mediaUrl;
    private String replyToMessageId;
    private boolean isEdited;
    private String senderName;
    private String senderAvatar;
    private boolean isDeleted;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
    @Builder.Default
    private String eventType = "MESSAGE"; // ADDED
}