package com.talkifyx.chat_service.client;

import com.talkifyx.chat_service.client.fallback.RoomServiceFallbackFactory;
import com.talkifyx.chat_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "room-service", configuration = FeignConfig.class,
        fallbackFactory = RoomServiceFallbackFactory.class)
public interface RoomServiceClient {

    @GetMapping("/api/rooms/{roomId}/members")
    Object getRoomMembers(@PathVariable Long roomId);
}