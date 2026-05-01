package com.talkifyx.message_service.resource;

import com.talkifyx.message_service.dto.*;
import com.talkifyx.message_service.entity.DeliveryStatus;
import com.talkifyx.message_service.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Message API")
public class MessageResource {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageResponse> send(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid MessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(userId, request));
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse> getById(
            @PathVariable("messageId") String messageId) {
        return ResponseEntity.ok(messageService.getMessageById(messageId));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<PagedResponse<MessageResponse>> getByRoom(
            @PathVariable("roomId") Long roomId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(messageService.getMessagesByRoom(roomId, page, size, userId));
    }

    @GetMapping("/room/{roomId}/before")
    public ResponseEntity<List<MessageResponse>> getBefore(
            @PathVariable("roomId") Long roomId,
            @RequestParam("before") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(messageService.getMessagesBefore(roomId, before, userId));
    }

    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> edit(
            @PathVariable("messageId") String messageId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("content") String content) {
        return ResponseEntity.ok(messageService.editMessage(messageId, userId, content));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> delete(
            @PathVariable("messageId") String messageId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "type", defaultValue = "EVERYONE") String deleteType) {
        messageService.deleteMessage(messageId, userId, deleteType);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/room/{roomId}/search")
    public ResponseEntity<List<MessageResponse>> search(
            @PathVariable("roomId") Long roomId,
            @RequestParam("keyword") String keyword,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(messageService.searchMessages(roomId, keyword, userId));
    }

    @GetMapping("/room/{roomId}/count")
    public ResponseEntity<Long> count(
            @PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(messageService.getMessageCount(roomId));
    }

    @GetMapping("/room/{roomId}/unread")
    public ResponseEntity<Long> unread(
            @PathVariable("roomId") Long roomId,
            @RequestParam("after") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(messageService.getUnreadMessages(roomId, after, userId));
    }

    @PutMapping("/{messageId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable("messageId") String messageId,
            @RequestParam("status") DeliveryStatus status) {
        messageService.updateDeliveryStatus(messageId, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/room/{roomId}/mark-read")
    public ResponseEntity<Void> markRoomRead(
            @PathVariable("roomId") Long roomId,
            @RequestHeader("X-User-Id") Long userId) {
        messageService.markRoomMessagesRead(roomId, userId);
        return ResponseEntity.ok().build();
    }
}