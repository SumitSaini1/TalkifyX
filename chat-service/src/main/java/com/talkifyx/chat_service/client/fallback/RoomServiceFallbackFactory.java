package com.talkifyx.chat_service.client.fallback;

import com.talkifyx.chat_service.client.RoomServiceClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RoomServiceFallbackFactory implements FallbackFactory<RoomServiceClient> {

    @Override
    public RoomServiceClient create(Throwable cause) {
        log.error("RoomServiceClient fallback triggered: {}", cause.getMessage());
        return new RoomServiceClient() {
            @Override public List<Map<String, Object>> getRoomMembers(Long roomId) { return null; }
            @Override public void updateLastRead(Long roomId, Long userId) {}
            @Override public void updateLastMessageAt(Long roomId, String at) {}
        };
    }
}