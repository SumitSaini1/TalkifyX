package com.talkifyx.notification_service.feign;

import com.talkifyx.notification_service.dto.PresenceDto;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PresenceServiceFallbackFactory implements FallbackFactory<PresenceServiceClient> {

    @Override
    public PresenceServiceClient create(Throwable cause) {
        return userId -> {
            PresenceDto dto = new PresenceDto();
            dto.setUserId(userId);
            dto.setStatus("OFFLINE");
            return dto;
        };
    }
}