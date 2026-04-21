package com.talkifyx.auth_service.service;

import java.util.List;

import com.talkifyx.auth_service.dto.ChangePasswordRequest;
import com.talkifyx.auth_service.dto.LoginRequest;
import com.talkifyx.auth_service.dto.ProfileUpdateRequest;
import com.talkifyx.auth_service.dto.RegisterRequest;
import com.talkifyx.auth_service.entity.User;

public interface AuthService {
    User register(RegisterRequest request);
    String login(LoginRequest request);
    User getUserById(Long id);
    User getUserByEmail(String email);
    void updateProfile(String email, ProfileUpdateRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    List<User> searchUsers(String username);
    void updateStatus(String email, User.Status status);
}