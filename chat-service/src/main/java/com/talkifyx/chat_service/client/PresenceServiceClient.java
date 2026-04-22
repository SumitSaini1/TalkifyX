package com.talkifyx.chat_service.client;

import com.talkifyx.chat_service.client.fallback.PresenceServiceFallbackFactory;
import com.talkifyx.chat_service.config.FeignConfig;
import com.talkifyx.chat_service.payload.PresencePayload;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "presence-service", configuration = FeignConfig.class,
        fallbackFactory = PresenceServiceFallbackFactory.class)
public interface PresenceServiceClient {

    @PutMapping("/api/presence/{userId}")
    void updatePresence(@PathVariable Long userId, @RequestBody PresencePayload payload);

    @GetMapping("/api/presence/{userId}")
    Object getPresence(@PathVariable Long userId);
}