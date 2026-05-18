package com.talkifyx.notification_service.feign;

import com.talkifyx.notification_service.dto.UserProfileDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceFallbackFactoryTest {

    @Test
    void create_ReturnsFallbackClient_WithNullFcmToken() {
        AuthServiceFallbackFactory factory = new AuthServiceFallbackFactory();
        Throwable cause = new RuntimeException("Auth service down");

        AuthServiceClient fallback = factory.create(cause);
        UserProfileDto result = fallback.getUserProfile("1");

        assertNotNull(result);
        assertNull(result.getFcmToken());
    }

    @Test
    void create_FallbackCalledWithDifferentUserId_StillReturnsDto() {
        AuthServiceFallbackFactory factory = new AuthServiceFallbackFactory();
        AuthServiceClient fallback = factory.create(new RuntimeException("timeout"));

        UserProfileDto result = fallback.getUserProfile("999");

        assertNotNull(result);
        assertNull(result.getFcmToken());
    }
}