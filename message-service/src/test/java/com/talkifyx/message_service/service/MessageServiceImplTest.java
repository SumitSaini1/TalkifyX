package com.talkifyx.message_service.service;

import com.talkifyx.message_service.dto.*;
import com.talkifyx.message_service.entity.*;
import com.talkifyx.message_service.feign.AuthServiceClient;
import com.talkifyx.message_service.feign.RoomServiceClient;
import com.talkifyx.message_service.repository.MessageReactionRepository;
import com.talkifyx.message_service.repository.MessageRepository;
import com.talkifyx.message_service.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceImplTest {

    @Mock private MessageRepository messageRepository;
    @Mock private MessageReactionRepository reactionRepository;
    @Mock private RoomServiceClient roomServiceClient;
    @Mock private AuthServiceClient authServiceClient;

    @InjectMocks
    private MessageServiceImpl messageService;

    private Message testMessage;
    private MessageRequest testRequest;

    @BeforeEach
    void setUp() {
        testMessage = Message.builder()
                .messageId("msg-001")
                .roomId(1L)
                .senderId(10L)
                .content("Hello World")
                .type(MessageType.TEXT)
                .deliveryStatus(DeliveryStatus.SENT)
                .build();

        testRequest = MessageRequest.builder()
                .roomId(1L)
                .content("Hello World")
                .type(MessageType.TEXT)
                .senderName("Test User")
                .senderAvatar("http://avatar.url")
                .build();

        // Default stub: no reactions, no auth lookup failure
        when(reactionRepository.findByMessageId(anyString())).thenReturn(Collections.emptyList());
        when(authServiceClient.getUserById(anyLong())).thenReturn(null);
    }

    // ===================== SEND MESSAGE =====================

    @Test
    void sendMessage_Success_ReturnsMessageResponse() {
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        MessageResponse response = messageService.sendMessage(10L, testRequest);

        assertNotNull(response);
        assertEquals("msg-001", response.getMessageId());
        assertEquals("Test User", response.getSenderName()); // from request override
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_SetsRoomIdAndSenderId() {
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        MessageResponse response = messageService.sendMessage(10L, testRequest);

        assertEquals(1L, response.getRoomId());
        assertEquals(10L, response.getSenderId());
    }

    // ===================== GET MESSAGE BY ID =====================

    @Test
    void getMessageById_Found_ReturnsResponse() {
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));

        MessageResponse response = messageService.getMessageById("msg-001");

        assertNotNull(response);
        assertEquals("msg-001", response.getMessageId());
    }

    @Test
    void getMessageById_NotFound_ThrowsResponseStatusException() {
        when(messageRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> messageService.getMessageById("bad-id"));
    }

    // ===================== GET MESSAGES BY ROOM =====================

    @Test
    void getMessagesByRoom_ReturnsPaged() {
        Page<Message> page = new PageImpl<>(List.of(testMessage));
        when(messageRepository.findVisibleMessagesByRoomId(eq(1L), eq(10L), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<MessageResponse> result = messageService.getMessagesByRoom(1L, 0, 20, 10L);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getMessagesByRoom_Empty_ReturnsEmptyContent() {
        Page<Message> emptyPage = new PageImpl<>(Collections.emptyList());
        when(messageRepository.findVisibleMessagesByRoomId(eq(1L), eq(10L), any(Pageable.class)))
                .thenReturn(emptyPage);

        PagedResponse<MessageResponse> result = messageService.getMessagesByRoom(1L, 0, 20, 10L);

        assertTrue(result.getContent().isEmpty());
    }

    // ===================== EDIT MESSAGE =====================

    @Test
    void editMessage_ByOwner_Success() {
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        MessageResponse response = messageService.editMessage("msg-001", 10L, "Updated content");

        assertTrue(testMessage.isEdited());
        assertEquals("Updated content", testMessage.getContent());
        assertNotNull(testMessage.getEditedAt());
    }

    @Test
    void editMessage_NotOwner_ThrowsForbidden() {
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> messageService.editMessage("msg-001", 99L, "hacked content"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void editMessage_DeletedMessage_ThrowsBadRequest() {
        testMessage.setDeleted(true);
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> messageService.editMessage("msg-001", 10L, "new content"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void editMessage_NotFound_ThrowsNotFound() {
        when(messageRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> messageService.editMessage("bad-id", 10L, "content"));
    }

    // ===================== DELETE MESSAGE =====================

    @Test
    void deleteMessage_ForEveryone_ByOwner_SetsDeleted() {
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        messageService.deleteMessage("msg-001", 10L, "EVERYONE");

        assertTrue(testMessage.isDeleted());
        assertEquals("This message was deleted", testMessage.getContent());
        assertNull(testMessage.getMediaUrl());
    }

    @Test
    void deleteMessage_ForEveryone_NotOwner_ThrowsForbidden() {
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> messageService.deleteMessage("msg-001", 99L, "EVERYONE"));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void deleteMessage_ForMe_AddsToDeletedForUsers() {
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        messageService.deleteMessage("msg-001", 10L, "ME");

        assertTrue(testMessage.getDeletedForUsers().contains(10L));
        assertFalse(testMessage.isDeleted()); // global delete flag stays false
    }

    @Test
    void deleteMessage_NotFound_ThrowsNotFound() {
        when(messageRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> messageService.deleteMessage("bad-id", 10L, "EVERYONE"));
    }

    // ===================== DELIVERY STATUS =====================

    @Test
    void updateDeliveryStatus_Success() {
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        messageService.updateDeliveryStatus("msg-001", DeliveryStatus.READ);

        assertEquals(DeliveryStatus.READ, testMessage.getDeliveryStatus());
    }

    @Test
    void updateDeliveryStatus_NotFound_ThrowsException() {
        when(messageRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> messageService.updateDeliveryStatus("bad-id", DeliveryStatus.READ));
    }

    @Test
    void markRoomMessagesRead_CallsBulkUpdate() {
        when(messageRepository.bulkUpdateDeliveryStatus(1L, 10L, DeliveryStatus.READ)).thenReturn(5);

        messageService.markRoomMessagesRead(1L, 10L);

        verify(messageRepository).bulkUpdateDeliveryStatus(1L, 10L, DeliveryStatus.READ);
    }

    // ===================== COUNT / UNREAD =====================

    @Test
    void getMessageCount_ReturnsCount() {
        when(messageRepository.countByRoomIdAndIsDeletedFalse(1L)).thenReturn(42L);

        long count = messageService.getMessageCount(1L);

        assertEquals(42L, count);
    }

    @Test
    void getUnreadMessages_ReturnsCount() {
        LocalDateTime after = LocalDateTime.now().minusHours(1);
        when(messageRepository.countUnreadMessages(1L, after, 10L)).thenReturn(7L);

        long count = messageService.getUnreadMessages(1L, after, 10L);

        assertEquals(7L, count);
    }

    // ===================== SEARCH =====================

    @Test
    void searchMessages_ReturnsMatchingList() {
        when(messageRepository.searchInRoom(1L, "hello", 10L)).thenReturn(List.of(testMessage));

        List<MessageResponse> results = messageService.searchMessages(1L, "hello", 10L);

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void searchMessages_NoMatch_ReturnsEmptyList() {
        when(messageRepository.searchInRoom(1L, "xyz", 10L)).thenReturn(Collections.emptyList());

        List<MessageResponse> results = messageService.searchMessages(1L, "xyz", 10L);

        assertTrue(results.isEmpty());
    }

    // ===================== REACT TO MESSAGE =====================

    @Test
    void reactToMessage_NewReaction_SavesReaction() {
        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));
        when(reactionRepository.findByMessageIdAndUserId("msg-001", 10L)).thenReturn(Optional.empty());
        when(reactionRepository.save(any(MessageReaction.class))).thenReturn(
                MessageReaction.builder().messageId("msg-001").userId(10L).emoji("👍").build());

        List<ReactionGroupDto> result = messageService.reactToMessage("msg-001", 10L, "👍");

        assertNotNull(result);
        verify(reactionRepository).save(any(MessageReaction.class));
    }

    @Test
    void reactToMessage_SameEmoji_TogglesOff() {
        MessageReaction existing = MessageReaction.builder()
                .id(1L).messageId("msg-001").userId(10L).emoji("👍").build();

        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));
        when(reactionRepository.findByMessageIdAndUserId("msg-001", 10L)).thenReturn(Optional.of(existing));
        doNothing().when(reactionRepository).deleteByMessageIdAndUserId("msg-001", 10L);

        messageService.reactToMessage("msg-001", 10L, "👍");

        verify(reactionRepository).deleteByMessageIdAndUserId("msg-001", 10L);
    }

    @Test
    void reactToMessage_DifferentEmoji_SwitchesEmoji() {
        MessageReaction existing = MessageReaction.builder()
                .id(1L).messageId("msg-001").userId(10L).emoji("👍").build();

        when(messageRepository.findById("msg-001")).thenReturn(Optional.of(testMessage));
        when(reactionRepository.findByMessageIdAndUserId("msg-001", 10L)).thenReturn(Optional.of(existing));
        when(reactionRepository.save(any(MessageReaction.class))).thenReturn(existing);

        messageService.reactToMessage("msg-001", 10L, "❤️");

        assertEquals("❤️", existing.getEmoji());
        verify(reactionRepository).save(existing);
    }

    @Test
    void reactToMessage_MessageNotFound_ThrowsException() {
        when(messageRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> messageService.reactToMessage("bad-id", 10L, "👍"));
    }
}
