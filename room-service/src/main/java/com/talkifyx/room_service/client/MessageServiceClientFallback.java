package com.talkifyx.room_service.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageServiceClientFallback implements FallbackFactory<MessageServiceClient> {
    @Override
    public MessageServiceClient create(Throwable cause) {
        return (roomId, after) -> 0L;
    }
}