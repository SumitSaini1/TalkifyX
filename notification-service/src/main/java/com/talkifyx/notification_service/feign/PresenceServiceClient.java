package com.talkifyx.notification_service.feign;

import com.talkifyx.notification_service.dto.PresenceDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PRESENCE-SERVICE", fallbackFactory = PresenceServiceFallbackFactory.class)
public interface PresenceServiceClient {

    @GetMapping("/api/presence/{userId}")
    PresenceDto getPresence(@PathVariable("userId") Long userId);
}