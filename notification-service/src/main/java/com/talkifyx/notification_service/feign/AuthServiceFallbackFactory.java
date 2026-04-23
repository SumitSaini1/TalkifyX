package com.talkifyx.notification_service.feign;

import com.talkifyx.notification_service.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceFallbackFactory implements FallbackFactory<AuthServiceClient> {

    @Override
    public AuthServiceClient create(Throwable cause) {
        return userId -> {
            UserProfileDto dto = new UserProfileDto();
            dto.setFcmToken(null);
            return dto;
        };
    }
}