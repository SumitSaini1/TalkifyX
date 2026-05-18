package com.talkifyx.notification_service.feign;

import com.talkifyx.notification_service.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient(name = "AUTH-SERVICE", fallbackFactory = AuthServiceFallbackFactory.class)
public interface AuthServiceClient {

    @GetMapping("/api/auth/internal/user/{id}")
    UserProfileDto getUserProfile(@PathVariable("id") String userId);
}