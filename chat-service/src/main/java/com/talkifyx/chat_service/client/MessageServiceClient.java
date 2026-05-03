package com.talkifyx.chat_service.client;

import com.talkifyx.chat_service.client.fallback.MessageServiceFallbackFactory;
import com.talkifyx.chat_service.config.FeignConfig;
import com.talkifyx.chat_service.payload.MessageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "message-service", configuration = FeignConfig.class,
        fallbackFactory = MessageServiceFallbackFactory.class)
public interface MessageServiceClient {

    @PostMapping("/api/messages")
    Object saveMessage(@RequestBody MessageRequest request, @RequestHeader("X-User-Id") Long userId);

    @PutMapping("/api/messages/{messageId}")
    Object editMessage(@PathVariable("messageId") String messageId, @RequestParam("content") String content);

    @DeleteMapping("/api/messages/{messageId}")
    void deleteMessage(@PathVariable("messageId") String messageId, @RequestHeader("X-User-Id") Long userId, @RequestParam(value = "type", defaultValue = "EVERYONE") String deleteType);

    @PutMapping("/api/messages/{messageId}/status")
    void updateStatus(@PathVariable("messageId") String messageId, @RequestParam("status") String status);

    @PostMapping("/api/messages/{messageId}/react")
    Object reactToMessage(@PathVariable("messageId") String messageId,
                          @RequestHeader("X-User-Id") Long userId,
                          @RequestParam("emoji") String emoji);

    @PutMapping("/api/messages/room/{roomId}/mark-read")
    void markRoomRead(@PathVariable("roomId") Long roomId, @RequestHeader("X-User-Id") Long readerId);
}