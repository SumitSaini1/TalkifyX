package com.talkifyx.presence_service.client;

import com.talkifyx.presence_service.dto.PresenceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "CHAT-SERVICE")
public interface ChatNotifyClient {

    @PostMapping("/api/ws/notify/presence")
    void notifyPresence(@RequestBody PresenceResponse payload);
}