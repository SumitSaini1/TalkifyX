package com.talkifyx.room_service.client;

import com.talkifyx.room_service.dto.ApiResponse;
import com.talkifyx.room_service.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth.service.url:http://localhost:8081}")
public interface AuthServiceClient {
    @GetMapping("/api/auth/internal/user/{id}")
    ApiResponse<UserDto> getUserById(@PathVariable Long id);
}