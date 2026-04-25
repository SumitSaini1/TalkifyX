package com.talkifyx.chat_service.client.fallback;

import com.talkifyx.chat_service.client.NotificationServiceClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class NotificationServiceFallbackFactory implements FallbackFactory<NotificationServiceClient> {

    @Override
    public NotificationServiceClient create(Throwable cause) {
        return payload -> {};
    }
}