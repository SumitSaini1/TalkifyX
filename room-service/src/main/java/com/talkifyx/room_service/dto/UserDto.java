package com.talkifyx.room_service.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String status;
}