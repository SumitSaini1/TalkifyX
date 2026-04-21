package com.talkifyx.room_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomMemberResponse {
    private Long memberId;
    private Long roomId;
    private Long userId;
    private String role;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private Boolean isMuted;
}