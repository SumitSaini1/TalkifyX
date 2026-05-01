package com.talkifyx.chat_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller called by room-service (via Feign) to broadcast WebSocket events
 * to individual users' personal topics when a new room is created.
 */
@RestController
@RequestMapping("/api/ws/notify")
@RequiredArgsConstructor
public class WsNotifyController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a NEW_ROOM event to all member user IDs.
     * Called by room-service after createRoom().
     *
     * Request body: { "room": {...}, "memberIds": [1, 2, ...] }
     */
    @PostMapping("/new-room")
    public void notifyNewRoom(@RequestBody Map<String, Object> body) {
        Object room = body.get("room");
        Object memberIdsRaw = body.get("memberIds");

        if (room == null || memberIdsRaw == null) return;

        @SuppressWarnings("unchecked")
        List<Object> memberIds = (List<Object>) memberIdsRaw;

        Map<String, Object> event = Map.of(
                "eventType", "NEW_ROOM",
                "room", room
        );

        for (Object memberId : memberIds) {
            Long uid = Long.valueOf(memberId.toString());
            messagingTemplate.convertAndSend("/topic/user/" + uid, event);
        }
    }

    /**
     * Broadcast a PRESENCE event to the global /topic/presence.
     * Called by presence-service when a user's status changes.
     */
    @PostMapping("/presence")
    public void notifyPresence(@RequestBody Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/presence", payload);
    }
}

