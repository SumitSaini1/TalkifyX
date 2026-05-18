package com.talkifyx.chat_service.client;

import com.talkifyx.chat_service.client.fallback.RoomServiceFallbackFactory;
import com.talkifyx.chat_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "room-service", configuration = FeignConfig.class,
        fallbackFactory = RoomServiceFallbackFactory.class)
public interface RoomServiceClient {

    @GetMapping("/api/rooms/{roomId}/members")
    List<Map<String, Object>> getRoomMembers(@PathVariable("roomId") Long roomId);

    @PutMapping("/api/rooms/{roomId}/read")
    void updateLastRead(@PathVariable("roomId") Long roomId, @RequestHeader("X-User-Id") Long userId);

    @PutMapping("/api/rooms/{roomId}/last-message-at")
    void updateLastMessageAt(@PathVariable("roomId") Long roomId, @RequestParam("at") String at);
}