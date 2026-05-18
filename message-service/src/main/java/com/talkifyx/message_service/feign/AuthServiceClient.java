package com.talkifyx.message_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.talkifyx.message_service.dto.ApiResponse;
import com.talkifyx.message_service.dto.SenderDto;

@FeignClient(name = "AUTH-SERVICE")
public interface AuthServiceClient {
    @GetMapping("/api/auth/internal/user/{id}")
    ApiResponse<SenderDto> getUserById(@PathVariable("id") Long id);
}