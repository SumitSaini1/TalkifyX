package com.talkifyx.message_service.dto;

import com.talkifyx.message_service.entity.DeliveryStatus;
import com.talkifyx.message_service.entity.MessageType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private MessageResponse replyToMessage;   // nested preview — never recursively populated
    private boolean isEdited;
    private String senderName;
    private String senderAvatar;
    private boolean isDeleted;
    private DeliveryStatus deliveryStatus;
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
    @Builder.Default
    private String eventType = "MESSAGE"; // ADDED
    @Builder.Default
    private List<ReactionGroupDto> reactions = new ArrayList<>();
}