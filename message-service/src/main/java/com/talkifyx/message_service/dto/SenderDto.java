package com.talkifyx.message_service.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SenderDto {
    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
}