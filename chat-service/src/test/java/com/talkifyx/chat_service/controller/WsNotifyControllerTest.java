package com.talkifyx.chat_service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WsNotifyControllerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private WsNotifyController controller;

    // ===================== NEW ROOM =====================

    @Test
    void notifyNewRoom_BroadcastsToEachMember() {
        Map<String, Object> body = Map.of(
                "room", Map.of("roomId", 1, "name", "Test Room"),
                "memberIds", List.of(10, 20, 30)
        );

        controller.notifyNewRoom(body);

        verify(messagingTemplate).convertAndSend(eq("/topic/user/10"), any(Map.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/20"), any(Map.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/30"), any(Map.class));
    }

    @Test
    void notifyNewRoom_NullRoom_DoesNotBroadcast() {
        Map<String, Object> body = Map.of("memberIds", List.of(10, 20));
        // "room" key is missing

        controller.notifyNewRoom(body);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void notifyNewRoom_NullMemberIds_DoesNotBroadcast() {
        Map<String, Object> body = Map.of("room", Map.of("roomId", 1));
        // "memberIds" key is missing

        controller.notifyNewRoom(body);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void notifyNewRoom_EmptyMemberList_NoBroadcast() {
        Map<String, Object> body = Map.of(
                "room", Map.of("roomId", 1),
                "memberIds", List.of()
        );

        controller.notifyNewRoom(body);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    // ===================== PRESENCE =====================

    @Test
    void notifyPresence_BroadcastsToGlobalPresenceTopic() {
        Map<String, Object> payload = Map.of("userId", 10, "status", "ONLINE");

        controller.notifyPresence(payload);

        verify(messagingTemplate).convertAndSend("/topic/presence", payload);
    }

    @Test
    void notifyPresence_AnyPayload_AlwaysBroadcasts() {
        Map<String, Object> payload = Map.of("userId", 99, "status", "AWAY");

        controller.notifyPresence(payload);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/presence", payload);
    }
}
