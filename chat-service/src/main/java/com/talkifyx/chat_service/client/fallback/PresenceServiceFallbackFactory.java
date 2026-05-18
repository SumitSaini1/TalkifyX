package com.talkifyx.chat_service.client.fallback;

import com.talkifyx.chat_service.client.PresenceServiceClient;
import com.talkifyx.chat_service.payload.PresencePayload;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PresenceServiceFallbackFactory implements FallbackFactory<PresenceServiceClient> {

    @Override
    public PresenceServiceClient create(Throwable cause) {
        return new PresenceServiceClient() {
            @Override public void updatePresence(Long id, PresencePayload p) {}
            @Override public Object getPresence(Long id) { return null; }
        };
    }
}