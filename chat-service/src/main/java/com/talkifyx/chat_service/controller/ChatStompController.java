package com.talkifyx.chat_service.controller;

import com.talkifyx.chat_service.handler.ChatWebSocketHandler;
import com.talkifyx.chat_service.payload.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Tag(name = "Chat STOMP", description = "STOMP WebSocket endpoints")
public class ChatStompController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @MessageMapping("/chat.send")
    @SendTo("/topic/room/{roomId}")
    @Operation(summary = "Send chat message")
    public ChatPayload sendMessage(@Payload ChatPayload payload,
                                   @Header("X-User-Id") String userId) {
        payload.setSenderId(Long.parseLong(userId));
        return payload;
    }

    @MessageMapping("/chat.typing")
    @Operation(summary = "Typing indicator")
    public void typing(@Payload TypingPayload payload,
                       @Header("X-User-Id") String userId) {
        payload.setSenderId(Long.parseLong(userId));
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
    }

    @MessageMapping("/chat.read")
    @Operation(summary = "Read receipt")
    public void readReceipt(@Payload ReadReceiptPayload payload,
                            @Header("X-User-Id") String userId) {
        payload.setReaderId(Long.parseLong(userId));
        messagingTemplate.convertAndSend("/topic/room/" + payload.getRoomId(), payload);
    }
}