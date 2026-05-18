package com.talkifyx.auth_service.service;

import com.talkifyx.auth_service.dto.*;
import com.talkifyx.auth_service.entity.User;
import com.talkifyx.auth_service.exception.*;
import com.talkifyx.auth_service.jwt.JwtUtil;
import com.talkifyx.auth_service.repository.UserRepository;
import com.talkifyx.auth_service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .fullName("Test User")
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .provider("LOCAL")
                .status(User.Status.ONLINE)
                .build();
    }

    // ===================== REGISTER =====================

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = authService.register(request);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_UsernameAlreadyTaken_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("new@example.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_EmailAlreadyRegistered_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("test@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    // ===================== LOGIN =====================

    @Test
    void login_Success_ReturnsToken() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken("test@example.com", 1L)).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        String token = authService.login(request);

        assertNotNull(token);
        assertEquals("jwt-token", token);
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    // ===================== GET USER =====================

    @Test
    void getUserById_Found_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = authService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getUserById(99L));
    }

    @Test
    void getUserByEmail_Found_ReturnsUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = authService.getUserByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserByEmail_NotFound_ThrowsException() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.getUserByEmail("missing@example.com"));
    }

    // ===================== UPDATE PROFILE =====================

    @Test
    void updateProfile_Success_UpdatesFields() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("New Name");
        request.setAvatarUrl("http://avatar.url");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.updateProfile("test@example.com", request);

        assertEquals("New Name", testUser.getFullName());
        assertEquals("http://avatar.url", testUser.getAvatarUrl());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateProfile_UsernameConflict_ThrowsException() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setUsername("takenuser");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> authService.updateProfile("test@example.com", request));
    }

    // ===================== CHANGE PASSWORD =====================

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newEncodedPass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.changePassword("test@example.com", request);

        assertEquals("newEncodedPass", testUser.getPassword());
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongOld");
        request.setNewPassword("newPass123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOld", "encodedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.changePassword("test@example.com", request));
    }

    // ===================== SEARCH & STATUS =====================

    @Test
    void searchUsers_ReturnsMatchingUsers() {
        when(userRepository.searchByUsername("test")).thenReturn(List.of(testUser));

        List<User> result = authService.searchUsers("test");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void updateStatus_UserExists_UpdatesStatus() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.updateStatus("test@example.com", User.Status.AWAY);

        assertEquals(User.Status.AWAY, testUser.getStatus());
    }

    @Test
    void updateStatus_UserNotFound_DoesNothing() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Should not throw, just silently skip
        assertDoesNotThrow(() -> authService.updateStatus("unknown@example.com", User.Status.AWAY));
        verify(userRepository, never()).save(any());
    }
}
