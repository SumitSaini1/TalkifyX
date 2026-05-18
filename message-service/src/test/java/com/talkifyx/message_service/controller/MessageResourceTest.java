package com.talkifyx.message_service.controller;

import com.talkifyx.message_service.dto.*;
import com.talkifyx.message_service.entity.DeliveryStatus;
import com.talkifyx.message_service.entity.MessageType;
import com.talkifyx.message_service.resource.MessageResource;
import com.talkifyx.message_service.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageResourceTest {

    @Mock private MessageService messageService;
    @InjectMocks private MessageResource messageResource;

    private MessageResponse testResponse;

    @BeforeEach
    void setUp() {
        testResponse = MessageResponse.builder()
                .messageId("msg-001")
                .roomId(1L)
                .senderId(10L)
                .content("Hello")
                .type(MessageType.TEXT)
                .build();
    }

    @Test
    void send_Returns201WithResponse() {
        MessageRequest request = MessageRequest.builder()
                .roomId(1L).type(MessageType.TEXT).content("Hello").build();
        when(messageService.sendMessage(10L, request)).thenReturn(testResponse);

        ResponseEntity<MessageResponse> response = messageResource.send(10L, request);

        assertEquals(201, response.getStatusCodeValue());
        assertEquals("msg-001", response.getBody().getMessageId());
    }

    @Test
    void getById_Found_Returns200() {
        when(messageService.getMessageById("msg-001")).thenReturn(testResponse);

        ResponseEntity<MessageResponse> response = messageResource.getById("msg-001");

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(messageService.getMessageById("bad-id"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> messageResource.getById("bad-id"));
    }

    @Test
    void getByRoom_Returns200WithPagedResponse() {
        PagedResponse<MessageResponse> paged = PagedResponse.<MessageResponse>builder()
                .content(List.of(testResponse)).page(0).size(20).totalElements(1).totalPages(1).last(true).build();
        when(messageService.getMessagesByRoom(1L, 0, 20, 10L)).thenReturn(paged);

        ResponseEntity<PagedResponse<MessageResponse>> response = messageResource.getByRoom(1L, 0, 20, 10L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void getBefore_Returns200WithList() {
        LocalDateTime before = LocalDateTime.now();
        when(messageService.getMessagesBefore(1L, before, 10L)).thenReturn(List.of(testResponse));

        ResponseEntity<List<MessageResponse>> response = messageResource.getBefore(1L, before, 10L);

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void edit_Success_Returns200() {
        when(messageService.editMessage("msg-001", 10L, "updated")).thenReturn(testResponse);

        ResponseEntity<MessageResponse> response = messageResource.edit("msg-001", 10L, "updated");

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void edit_NotOwner_ThrowsForbidden() {
        when(messageService.editMessage("msg-001", 99L, "hack"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> messageResource.edit("msg-001", 99L, "hack"));
    }

    @Test
    void delete_Success_Returns204() {
        doNothing().when(messageService).deleteMessage("msg-001", 10L, "EVERYONE");

        ResponseEntity<Void> response = messageResource.delete("msg-001", 10L, "EVERYONE");

        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void search_Returns200WithList() {
        when(messageService.searchMessages(1L, "hello", 10L)).thenReturn(List.of(testResponse));

        ResponseEntity<List<MessageResponse>> response = messageResource.search(1L, "hello", 10L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void count_Returns200WithCount() {
        when(messageService.getMessageCount(1L)).thenReturn(10L);

        ResponseEntity<Long> response = messageResource.count(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(10L, response.getBody());
    }

    @Test
    void unread_Returns200WithCount() {
        LocalDateTime after = LocalDateTime.now().minusHours(1);
        when(messageService.getUnreadMessages(1L, after, 10L)).thenReturn(3L);

        ResponseEntity<Long> response = messageResource.unread(1L, after, 10L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(3L, response.getBody());
    }

    @Test
    void updateStatus_Returns200() {
        doNothing().when(messageService).updateDeliveryStatus("msg-001", DeliveryStatus.READ);

        ResponseEntity<Void> response = messageResource.updateStatus("msg-001", DeliveryStatus.READ);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void markRoomRead_Returns200() {
        doNothing().when(messageService).markRoomMessagesRead(1L, 10L);

        ResponseEntity<Void> response = messageResource.markRoomRead(1L, 10L);

        assertEquals(200, response.getStatusCodeValue());
        verify(messageService).markRoomMessagesRead(1L, 10L);
    }

    @Test
    void react_Returns200WithReactionList() {
        when(messageService.reactToMessage("msg-001", 10L, "👍"))
                .thenReturn(List.of(ReactionGroupDto.builder().emoji("👍").count(1).userIds(List.of(10L)).build()));

        ResponseEntity<List<ReactionGroupDto>> response = messageResource.react("msg-001", 10L, "👍");

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("👍", response.getBody().get(0).getEmoji());
    }

    @Test
    void react_MessageNotFound_ThrowsException() {
        when(messageService.reactToMessage("bad-id", 10L, "👍"))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class,
                () -> messageResource.react("bad-id", 10L, "👍"));
    }
}
