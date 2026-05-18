package com.talkifyx.room_service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomResponse {
    private Long roomId;
    private String name;
    private String description;
    private String type;
    private Long createdById;
    private String avatarUrl;
    private UserDto otherUser;
    private Boolean isPrivate;
    private Integer maxMembers;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private long memberCount;
}