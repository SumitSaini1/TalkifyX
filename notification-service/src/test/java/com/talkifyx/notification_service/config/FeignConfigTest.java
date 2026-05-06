package com.talkifyx.notification_service.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class FeignConfigTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requestInterceptor_WithAuthAndUserId_AddsHeaders() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", "Bearer token123");
        mockRequest.addHeader("X-User-Id", "5");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        FeignConfig config = new FeignConfig();
        RequestTemplate template = new RequestTemplate();
        config.requestInterceptor().apply(template);

        assertTrue(template.headers().containsKey("Authorization"));
        assertTrue(template.headers().containsKey("X-User-Id"));
    }

    @Test
    void requestInterceptor_WithNoHeaders_DoesNotAddHeaders() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        FeignConfig config = new FeignConfig();
        RequestTemplate template = new RequestTemplate();
        config.requestInterceptor().apply(template);

        assertFalse(template.headers().containsKey("Authorization"));
        assertFalse(template.headers().containsKey("X-User-Id"));
    }

    @Test
    void requestInterceptor_WithNoRequestContext_DoesNotThrow() {
        RequestContextHolder.resetRequestAttributes();

        FeignConfig config = new FeignConfig();
        RequestTemplate template = new RequestTemplate();

        assertDoesNotThrow(() -> config.requestInterceptor().apply(template));
    }
}