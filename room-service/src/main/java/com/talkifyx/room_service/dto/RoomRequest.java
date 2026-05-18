package com.talkifyx.room_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomRequest {
    @NotBlank private String name;
    private String description;
    @NotBlank private String type; // GROUP or DM
    private String avatarUrl;
    private Boolean isPrivate;
    private Integer maxMembers;
}