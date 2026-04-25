package com.talkifyx.chat_service.client.fallback;

import com.talkifyx.chat_service.client.RoomServiceClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RoomServiceFallbackFactory implements FallbackFactory<RoomServiceClient> {

    @Override
    public RoomServiceClient create(Throwable cause) {
        return roomId -> null;
    }
}