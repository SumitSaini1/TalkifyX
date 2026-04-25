package com.talkifyx.notification_service.feign;

import com.talkifyx.notification_service.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "AUTH-SERVICE", fallbackFactory = AuthServiceFallbackFactory.class)
public interface AuthServiceClient {

    @GetMapping("/api/auth/profile")
    UserProfileDto getUserProfile(@RequestHeader("X-User-Id") String userId);
}