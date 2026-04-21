package com.talkifyx.auth_service.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String fullName;
    private String avatarUrl;
    private String username;
}