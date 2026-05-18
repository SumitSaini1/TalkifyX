package com.talkifyx.auth_service.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Secret must be at least 32 chars for HS256
    private static final String SECRET = "test-secret-key-1234567890-abcdef";
    private static final long EXPIRY = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiry", EXPIRY);
    }

    @Test
    void generateAccessToken_ReturnsNonNullToken() {
        String token = jwtUtil.generateAccessToken("user@example.com", 1L);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractEmail_ReturnsCorrectEmail() {
        String token = jwtUtil.generateAccessToken("user@example.com", 1L);
        String email = jwtUtil.extractEmail(token);
        assertEquals("user@example.com", email);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtUtil.generateAccessToken("user@example.com", 1L);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_TamperedToken_ReturnsFalse() {
        String token = jwtUtil.generateAccessToken("user@example.com", 1L);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtUtil.validateToken(tampered));
    }
}
