package com.talkifyx.auth_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.talkifyx.auth_service.dto.ApiResponse;
import com.talkifyx.auth_service.dto.ChangePasswordRequest;
import com.talkifyx.auth_service.dto.LoginRequest;
import com.talkifyx.auth_service.dto.ProfileUpdateRequest;
import com.talkifyx.auth_service.dto.RegisterRequest;
import com.talkifyx.auth_service.entity.User;
import com.talkifyx.auth_service.jwt.JwtUtil;
import com.talkifyx.auth_service.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication & User Management")
public class AuthResource {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        Map<String, Object> data = Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "username", user.getUsername(),
                "email", user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", data));
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        User user = authService.getUserByEmail(
                jwtUtil.extractEmail(token));
        Map<String, Object> data = Map.of(
                "token", token,
                "type", "Bearer",
                "user", Map.of(
                        "id", user.getId(),
                        "fullName", user.getFullName(),
                        "username", user.getUsername(),
                        "email", user.getEmail()));
        return ResponseEntity.ok(ApiResponse.success("Login successful", data));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal String email) {
        if (email != null) {
            authService.updateStatus(email, User.Status.INVISIBLE);
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/internal/user/{id}")
    @Operation(summary = "Get user by ID (internal service call)")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long id) {
        User user = authService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User found", user));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate token")
    public ResponseEntity<ApiResponse> validate(@RequestParam String token) {
        boolean valid = jwtUtil.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token validated", Map.of("valid", valid)));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get profile")
    public ResponseEntity<ApiResponse> getProfile(@AuthenticationPrincipal String email) {
        User user = authService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", user));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile")
    public ResponseEntity<ApiResponse> updateProfile(@AuthenticationPrincipal String email,
            @RequestBody ProfileUpdateRequest request) {
        authService.updateProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", null));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse> changePassword(@AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users")
    public ResponseEntity<ApiResponse> search(@RequestParam String username) {
        List<User> users = authService.searchUsers(username);
        return ResponseEntity.ok(ApiResponse.success("Users found", users));
    }

    @PutMapping("/status")
    @Operation(summary = "Update status")
    public ResponseEntity<ApiResponse> updateStatus(@AuthenticationPrincipal String email,
            @RequestParam User.Status status) {
        authService.updateStatus(email, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", null));
    }
}