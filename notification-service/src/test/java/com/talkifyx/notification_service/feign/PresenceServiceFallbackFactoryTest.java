package com.talkifyx.notification_service.feign;

import com.talkifyx.notification_service.dto.PresenceDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PresenceServiceFallbackFactoryTest {

    @Test
    void create_ReturnsFallbackClient_WithOfflineStatus() {
        PresenceServiceFallbackFactory factory = new PresenceServiceFallbackFactory();
        Throwable cause = new RuntimeException("Presence service down");

        PresenceServiceClient fallback = factory.create(cause);
        PresenceDto result = fallback.getPresence(1L);

        assertNotNull(result);
        assertEquals("OFFLINE", result.getStatus());
        assertEquals(1L, result.getUserId());
    }

    @Test
    void create_FallbackCalledWithDifferentUserId_ReturnsCorrectUserId() {
        PresenceServiceFallbackFactory factory = new PresenceServiceFallbackFactory();
        PresenceServiceClient fallback = factory.create(new RuntimeException("timeout"));

        PresenceDto result = fallback.getPresence(42L);

        assertEquals(42L, result.getUserId());
        assertEquals("OFFLINE", result.getStatus());
    }
}