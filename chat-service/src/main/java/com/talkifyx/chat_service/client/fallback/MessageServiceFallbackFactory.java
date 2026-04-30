package com.talkifyx.chat_service.client.fallback;

import com.talkifyx.chat_service.client.MessageServiceClient;
import com.talkifyx.chat_service.payload.MessageRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageServiceFallbackFactory implements FallbackFactory<MessageServiceClient> {

    @Override
    public MessageServiceClient create(Throwable cause) {
        return new MessageServiceClient() {
            @Override public Object saveMessage(MessageRequest p, Long userId) { return null; }
            @Override public Object editMessage(String id, String c) { return null; }
            @Override public void deleteMessage(String id) {}
            @Override public void updateStatus(String id, String s) {}
            @Override public void markRoomRead(Long roomId, Long readerId) {}
        };
    }
}