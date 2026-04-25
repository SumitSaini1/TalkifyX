package com.talkifyx.room_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "message-service", fallbackFactory = MessageServiceClientFallback.class)
public interface MessageServiceClient {
    @GetMapping("/api/messages/room/{roomId}/unread-after")
    long getUnreadCount(@PathVariable Long roomId, @RequestParam String after);
}