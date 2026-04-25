package com.talkifyx.room_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private RoomType type;

    private Long createdById;
    private String avatarUrl;
    private Boolean isPrivate;
    private Integer maxMembers;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum RoomType { GROUP, DM }
}