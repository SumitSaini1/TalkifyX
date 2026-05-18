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
    private UserInfo user; 

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserInfo {
        private Long id;
        private String fullName;
        private String username;
        private String avatarUrl;
        private String status;
    }
}