package com.talkifyx.chat_service.client.fallback;

import com.talkifyx.chat_service.client.RoomServiceClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RoomServiceFallbackFactory implements FallbackFactory<RoomServiceClient> {

    @Override
    public RoomServiceClient create(Throwable cause) {
        log.error("RoomServiceClient fallback triggered: {}", cause.getMessage());
        return roomId -> null;
    }
}