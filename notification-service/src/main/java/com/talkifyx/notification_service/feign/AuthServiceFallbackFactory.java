package com.talkifyx.notification_service.feign;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.talkifyx.notification_service.dto.UserProfileDto;

@Slf4j
@Component
public class AuthServiceFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        return (userId) -> {
            log.error("AuthService fallback triggered for userId={} due to {}", userId, cause.getMessage());
            UserProfileDto dto = new UserProfileDto();
            dto.setFcmToken(null);
            return dto;
        };
    }
}