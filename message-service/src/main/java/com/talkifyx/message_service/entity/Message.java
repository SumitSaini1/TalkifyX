package com.talkifyx.message_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id", length = 36, nullable = false, updatable = false)
    private String messageId;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    private String mediaUrl;

    @Column(name = "reply_to_message_id", length = 36)
    private String replyToMessageId;

    @Builder.Default
    @Column(nullable = false)
    private boolean isEdited = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    private LocalDateTime editedAt;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }
}