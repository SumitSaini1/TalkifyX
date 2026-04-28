package com.talkifyx.chat_service.client;

import com.talkifyx.chat_service.client.fallback.MessageServiceFallbackFactory;
import com.talkifyx.chat_service.config.FeignConfig;
import com.talkifyx.chat_service.payload.ChatPayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "message-service", configuration = FeignConfig.class,
        fallbackFactory = MessageServiceFallbackFactory.class)
public interface MessageServiceClient {

    @PostMapping("/api/messages")
    Object saveMessage(@RequestBody ChatPayload payload,@RequestHeader("X-User-Id") Long userId);

    @PutMapping("/api/messages/{messageId}")
    Object editMessage(@PathVariable String messageId, @RequestParam String content);

    @DeleteMapping("/api/messages/{messageId}")
    void deleteMessage(@PathVariable String messageId);

    @PutMapping("/api/messages/{messageId}/status")
    void updateStatus(@PathVariable String messageId, @RequestParam String status);
}