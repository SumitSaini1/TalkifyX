package com.talkifyx.chat_service.client;

import com.talkifyx.chat_service.client.fallback.NotificationServiceFallbackFactory;
import com.talkifyx.chat_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "notification-service", configuration = FeignConfig.class,
        fallbackFactory = NotificationServiceFallbackFactory.class)
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/send")
    void sendNotification(@RequestBody Map<String, Object> payload);
}