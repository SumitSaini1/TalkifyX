package com.talkifyx.room_service.client;

import com.talkifyx.room_service.config.FeignConfig;
import com.talkifyx.room_service.dto.RoomResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "chat-service", configuration = FeignConfig.class)
public interface ChatNotifyClient {

    @PostMapping("/api/ws/notify/new-room")
    void notifyNewRoom(@RequestBody Map<String, Object> payload);

    default void notifyNewRoom(RoomResponse room, List<Long> memberIds) {
        notifyNewRoom(Map.of("room", room, "memberIds", memberIds));
    }
}
