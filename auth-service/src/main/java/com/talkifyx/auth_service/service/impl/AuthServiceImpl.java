package com.talkifyx.auth_service.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.talkifyx.auth_service.dto.ChangePasswordRequest;
import com.talkifyx.auth_service.dto.LoginRequest;
import com.talkifyx.auth_service.dto.ProfileUpdateRequest;
import com.talkifyx.auth_service.dto.RegisterRequest;
import com.talkifyx.auth_service.entity.User;
import com.talkifyx.auth_service.exception.InvalidCredentialsException;
import com.talkifyx.auth_service.exception.ResourceNotFoundException;
import com.talkifyx.auth_service.exception.UserAlreadyExistsException;
import com.talkifyx.auth_service.jwt.JwtUtil;
import com.talkifyx.auth_service.repository.UserRepository;
import com.talkifyx.auth_service.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterRequest request) {
        Map<String, String> errors = new HashMap<>();

        if (userRepository.existsByUsername(request.getUsername()))
            errors.put("username", "Username already taken");

        if (userRepository.existsByEmail(request.getEmail()))
            errors.put("email", "Email already registered");

        if (!errors.isEmpty())
            throw new UserAlreadyExistsException("User already exists");

        User user = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .status(User.Status.ONLINE)
                .build();

        return userRepository.save(user);
    }

    @Override
    public String login(LoginRequest request) {
        User user = userRepository
                .findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new InvalidCredentialsException("Invalid credentials");

        user.setStatus(User.Status.ONLINE);
        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtUtil.generateAccessToken(user.getEmail(), user.getId());
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public void updateProfile(String email, ProfileUpdateRequest request) {
        User user = getUserByEmail(email);
        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null)
            user.setAvatarUrl(request.getAvatarUrl());
        if (request.getFcmToken() != null)
            user.setFcmToken(request.getFcmToken());
        if (request.getUsername() != null) {
            if (!request.getUsername().equals(user.getUsername()) &&
                    userRepository.existsByUsername(request.getUsername()))
                throw new UserAlreadyExistsException("Username already taken");
            user.setUsername(request.getUsername());
        }   
        userRepository.save(user);
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserByEmail(email);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new InvalidCredentialsException("Old password incorrect");
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public List<User> searchUsers(String username) {
        return userRepository.searchByUsername(username);
    }

    @Override
    public void updateStatus(String email, User.Status status) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setStatus(status);
            user.setLastSeenAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}