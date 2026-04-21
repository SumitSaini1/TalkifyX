package com.talkifyx.room_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    private Long roomId;
    private Long userId;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private Boolean isMuted;

    @PrePersist
    public void prePersist() {
        this.joinedAt = LocalDateTime.now();
        if (this.isMuted == null) this.isMuted = false;
    }

    public enum MemberRole { ADMIN, MEMBER }
}