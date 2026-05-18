package com.talkifyx.notification_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwaggerConfigTest {

    @Test
    void notificationServiceOpenAPI_ReturnsValidOpenAPI() {
        SwaggerConfig config = new SwaggerConfig();
        OpenAPI openAPI = config.notificationServiceOpenAPI();

        assertNotNull(openAPI);
        assertEquals("Notification Service API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
    }

    @Test
    void notificationServiceOpenAPI_HasBearerSecurityScheme() {
        SwaggerConfig config = new SwaggerConfig();
        OpenAPI openAPI = config.notificationServiceOpenAPI();

        assertNotNull(openAPI.getComponents());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("Bearer"));
    }

    @Test
    void notificationServiceOpenAPI_HasSecurityRequirement() {
        SwaggerConfig config = new SwaggerConfig();
        OpenAPI openAPI = config.notificationServiceOpenAPI();

        assertFalse(openAPI.getSecurity().isEmpty());
    }
}