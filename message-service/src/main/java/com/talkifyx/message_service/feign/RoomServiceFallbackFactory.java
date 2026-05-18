package com.talkifyx.message_service.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.Collections;

@Component
public class RoomServiceFallbackFactory implements FallbackFactory<RoomServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceFallbackFactory.class);

    @Override
    public RoomServiceClient create(Throwable cause) {
        return roomId -> {
            log.error("room-service fallback roomId={}: {}", roomId, cause.getMessage());
            return ResponseEntity.ok(Collections.emptyList());
        };
    }
}