package com.talkifyx.chat_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talkifyx.chat_service.client.MessageServiceClient;
import com.talkifyx.chat_service.client.NotificationServiceClient;
import com.talkifyx.chat_service.client.PresenceServiceClient;
import com.talkifyx.chat_service.client.RoomServiceClient;
import com.talkifyx.chat_service.handler.ChatWebSocketHandler;
import com.talkifyx.chat_service.payload.ChatPayload;
import com.talkifyx.chat_service.payload.TypingPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatStompControllerTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ChatWebSocketHandler chatWebSocketHandler;
    @Mock private MessageServiceClient messageServiceClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private RoomServiceClient roomServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private PresenceServiceClient presenceServiceClient;

    @InjectMocks
    private ChatStompController controller;

    private final ObjectMapper realMapper = new ObjectMapper();

    

    @Test
    void sendMessage_SavesAndBroadcastsToRoom() throws Exception {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setContent("Hello");
        payload.setSenderName("Alice");

        String rawBody = realMapper.writeValueAsString(payload);
        when(objectMapper.readValue(rawBody, ChatPayload.class)).thenReturn(payload);

        Object savedMsg = Map.of("messageId", "msg-001", "content", "Hello");
        when(messageServiceClient.saveMessage(any(), eq(10L))).thenReturn(savedMsg);
        when(roomServiceClient.getRoomMembers(1L)).thenReturn(List.of());

        controller.sendMessage(rawBody, "10");

        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), eq(savedMsg));
    }

    @Test
    void sendMessage_MessageServiceReturnsNull_DoesNotBroadcast() throws Exception {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setContent("Hello");

        String rawBody = realMapper.writeValueAsString(payload);
        when(objectMapper.readValue(rawBody, ChatPayload.class)).thenReturn(payload);
        when(messageServiceClient.saveMessage(any(), eq(10L))).thenReturn(null);

        
        controller.sendMessage(rawBody, "10");

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void sendMessage_BroadcastsRoomUpdatedToEachMember() throws Exception {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setContent("Hi");

        String rawBody = realMapper.writeValueAsString(payload);
        when(objectMapper.readValue(rawBody, ChatPayload.class)).thenReturn(payload);

        Object savedMsg = Map.of("messageId", "msg-001");
        when(messageServiceClient.saveMessage(any(), eq(10L))).thenReturn(savedMsg);

        List<Map<String, Object>> members = List.of(
                Map.of("userId", 20),
                Map.of("userId", 30)
        );
        when(roomServiceClient.getRoomMembers(1L)).thenReturn(members);

        controller.sendMessage(rawBody, "10");

        
        verify(messagingTemplate).convertAndSend(eq("/topic/user/20"), any(Map.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/30"), any(Map.class));
    }

    @Test
    void sendMessage_OfflineMember_SendsPushNotification() throws Exception {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setContent("Hi");
        payload.setSenderName("Alice");

        String rawBody = realMapper.writeValueAsString(payload);
        when(objectMapper.readValue(rawBody, ChatPayload.class)).thenReturn(payload);
        when(messageServiceClient.saveMessage(any(), eq(10L))).thenReturn(Map.of("messageId", "m1"));

        List<Map<String, Object>> members = List.of(Map.of("userId", 20));
        when(roomServiceClient.getRoomMembers(1L)).thenReturn(members);
        // user 20 is OFFLINE
        when(presenceServiceClient.getPresence(20L)).thenReturn(Map.of("status", "OFFLINE"));

        controller.sendMessage(rawBody, "10");

        verify(notificationServiceClient).sendNotification(any(Map.class));
    }

    @Test
    void sendMessage_OnlineMember_SkipsPushNotification() throws Exception {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setContent("Hi");

        String rawBody = realMapper.writeValueAsString(payload);
        when(objectMapper.readValue(rawBody, ChatPayload.class)).thenReturn(payload);
        when(messageServiceClient.saveMessage(any(), eq(10L))).thenReturn(Map.of("messageId", "m1"));

        List<Map<String, Object>> members = List.of(Map.of("userId", 20));
        when(roomServiceClient.getRoomMembers(1L)).thenReturn(members);
       
        when(presenceServiceClient.getPresence(20L)).thenReturn(Map.of("status", "ONLINE"));

        controller.sendMessage(rawBody, "10");

        verify(notificationServiceClient, never()).sendNotification(any());
    }

    @Test
    void sendMessage_SenderInMemberList_SkipsSelfNotification() throws Exception {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setContent("Hi");

        String rawBody = realMapper.writeValueAsString(payload);
        when(objectMapper.readValue(rawBody, ChatPayload.class)).thenReturn(payload);
        when(messageServiceClient.saveMessage(any(), eq(10L))).thenReturn(Map.of("messageId", "m1"));

        
        List<Map<String, Object>> members = List.of(Map.of("userId", 10));
        when(roomServiceClient.getRoomMembers(1L)).thenReturn(members);

        controller.sendMessage(rawBody, "10");

       
        verify(notificationServiceClient, never()).sendNotification(any());
    }

    

    @Test
    void typing_SetsSenderIdAndBroadcasts() {
        TypingPayload payload = new TypingPayload();
        payload.setRoomId(5L);
        payload.setTyping(true);

        controller.typing(payload, "10");

        assertEquals(10L, payload.getSenderId());
        verify(messagingTemplate).convertAndSend("/topic/room/5", payload);
    }

  

    @Test
    void editMessage_SetsSenderIdAndBroadcastsToRoom() {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setMessageId("msg-001");

        controller.editMessage(payload, "10");

        assertEquals(10L, payload.getSenderId());
        verify(messagingTemplate).convertAndSend("/topic/room/1", payload);
    }

  

    @Test
    void deleteMessage_ForEveryone_BroadcastsToRoom() {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setDeleteType("EVERYONE");

        controller.deleteMessage(payload, "10");

        verify(messagingTemplate).convertAndSend("/topic/room/1", payload);
    }

    @Test
    void deleteMessage_ForMe_BroadcastsToUserTopic() {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setDeleteType("ME");

        controller.deleteMessage(payload, "10");

        verify(messagingTemplate).convertAndSend("/topic/user/10", payload);
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/room/1"), any(Object.class));
    }

    @Test
    void deleteMessage_NullDeleteType_DefaultsToEveryone() {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setDeleteType(null); 

        controller.deleteMessage(payload, "10");

        verify(messagingTemplate).convertAndSend("/topic/room/1", payload);
    }

   

    @Test
    void reactToMessage_SavesAndBroadcastsReactionEvent() {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setMessageId("msg-001");
        payload.setEmoji("👍");

        Object reactions = List.of(Map.of("emoji", "👍", "count", 1));
        when(messageServiceClient.reactToMessage("msg-001", 10L, "👍")).thenReturn(reactions);

        controller.reactToMessage(payload, "10");

        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), any(Map.class));
    }

    @Test
    void reactToMessage_ServiceReturnsNull_BroadcastsEmptyReactions() {
        ChatPayload payload = new ChatPayload();
        payload.setRoomId(1L);
        payload.setMessageId("msg-001");
        payload.setEmoji("❤️");

        when(messageServiceClient.reactToMessage("msg-001", 10L, "❤️")).thenReturn(null);

        controller.reactToMessage(payload, "10");

       
        verify(messagingTemplate).convertAndSend(eq("/topic/room/1"), any(Map.class));
    }

    

   
}
