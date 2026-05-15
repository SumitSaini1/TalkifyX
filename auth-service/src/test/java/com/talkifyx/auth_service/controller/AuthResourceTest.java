package com.talkifyx.auth_service.controller;

import com.talkifyx.auth_service.dto.*;
import com.talkifyx.auth_service.entity.User;
import com.talkifyx.auth_service.exception.InvalidCredentialsException;
import com.talkifyx.auth_service.exception.ResourceNotFoundException;
import com.talkifyx.auth_service.jwt.JwtUtil;
import com.talkifyx.auth_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthResource authResource;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .fullName("Test User")
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .status(User.Status.ONLINE)
                .build();
    }

    @Test
    void register_Success_Returns200() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("pass123");

        when(authService.register(request)).thenReturn(testUser);

        ResponseEntity<ApiResponse> response = authResource.register(request);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void login_Success_ReturnsTokenAndUser() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("pass123");

        when(authService.login(request)).thenReturn("jwt-token");
        when(jwtUtil.extractEmail("jwt-token")).thenReturn("test@example.com");
        when(authService.getUserByEmail("test@example.com")).thenReturn(testUser);

        ResponseEntity<ApiResponse> response = authResource.login(request);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void login_InvalidCredentials_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("bad@example.com");
        request.setPassword("wrong");

        when(authService.login(request)).thenThrow(new InvalidCredentialsException("Invalid credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authResource.login(request));
    }

    @Test
    void getUserById_Found_Returns200() {
        when(authService.getUserById(1L)).thenReturn(testUser);

        ResponseEntity<ApiResponse> response = authResource.getUserById(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody().getData());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(authService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found"));

        assertThrows(ResourceNotFoundException.class, () -> authResource.getUserById(99L));
    }

    @Test
    void validate_ValidToken_ReturnsTrue() {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);

        ResponseEntity<ApiResponse> response = authResource.validate("valid-token");

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void getProfile_ReturnsUser() {
        when(authService.getUserByEmail("test@example.com")).thenReturn(testUser);

        ResponseEntity<ApiResponse> response = authResource.getProfile("test@example.com");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(testUser, response.getBody().getData());
    }

    @Test
    void updateProfile_Success_Returns200() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("New Name");

        doNothing().when(authService).updateProfile("test@example.com", request);

        ResponseEntity<ApiResponse> response = authResource.updateProfile("test@example.com", request);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void changePassword_Success_Returns200() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("newpass");

        doNothing().when(authService).changePassword("test@example.com", request);

        ResponseEntity<ApiResponse> response = authResource.changePassword("test@example.com", request);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void logout_WithEmail_UpdatesStatus() {
        doNothing().when(authService).updateStatus("test@example.com", User.Status.INVISIBLE);

        ResponseEntity<ApiResponse> response = authResource.logout("test@example.com");

        assertEquals(200, response.getStatusCodeValue());
        verify(authService).updateStatus("test@example.com", User.Status.INVISIBLE);
    }

    @Test
    void logout_NullEmail_DoesNotCallUpdateStatus() {
        ResponseEntity<ApiResponse> response = authResource.logout(null);

        assertEquals(200, response.getStatusCodeValue());
        verify(authService, never()).updateStatus(any(), any());
    }

    @Test
    void updateStatus_Success_Returns200() {
        doNothing().when(authService).updateStatus("test@example.com", User.Status.AWAY);

        ResponseEntity<ApiResponse> response = authResource.updateStatus("test@example.com", User.Status.AWAY);

        assertEquals(200, response.getStatusCodeValue());
    }
}
