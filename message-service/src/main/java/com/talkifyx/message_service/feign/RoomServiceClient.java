package com.talkifyx.message_service.feign;

import com.talkifyx.message_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "room-service", configuration = FeignConfig.class,
             fallbackFactory = RoomServiceFallbackFactory.class)
public interface RoomServiceClient {

    @GetMapping("/api/rooms/{roomId}/members")
    ResponseEntity<List<Object>> getRoomMembers(@PathVariable("roomId") Long roomId);
}