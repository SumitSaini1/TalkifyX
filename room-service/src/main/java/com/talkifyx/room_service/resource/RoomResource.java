package com.talkifyx.room_service.resource;

import com.talkifyx.room_service.dto.*;
import com.talkifyx.room_service.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Service")
public class RoomResource {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "Create room")
    public ResponseEntity<RoomResponse> createRoom(
            @Valid @RequestBody RoomRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(roomService.createRoom(request, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    public ResponseEntity<RoomResponse> getRoomById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(roomService.getRoomById(id, userId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get rooms by user")
    public ResponseEntity<List<RoomResponse>> getRoomsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(roomService.getRoomsByUser(userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update room")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable Long id,
            @RequestBody RoomRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(roomService.updateRoom(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        roomService.deleteRoom(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member")
    public ResponseEntity<RoomMemberResponse> addMember(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestHeader("X-User-Id") Long requesterId) {
        return ResponseEntity.ok(roomService.addMember(id, userId, requesterId));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove member")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requesterId) {
        roomService.removeMember(id, userId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get members")
    public ResponseEntity<List<RoomMemberResponse>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getMembers(id));
    }

    @PutMapping("/{id}/members/{userId}/role")
    @Operation(summary = "Update member role")
    public ResponseEntity<RoomMemberResponse> updateRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam String role,
            @RequestHeader("X-User-Id") Long requesterId) {
        return ResponseEntity.ok(roomService.updateMemberRole(id, userId, role, requesterId));
    }

    @PutMapping("/{id}/members/{userId}/mute")
    @Operation(summary = "Mute/unmute member")
    public ResponseEntity<Void> muteUnmute(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam boolean mute,
            @RequestHeader("X-User-Id") Long requesterId) {
        roomService.muteUnmuteMember(id, userId, mute, requesterId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/unread/{userId}")
    @Operation(summary = "Get unread count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Long id,
            @PathVariable Long userId) {
        return ResponseEntity.ok(roomService.getUnreadCount(id, userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Update last read")
    public ResponseEntity<Void> updateLastRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        roomService.updateLastRead(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/last-message-at")
    @Operation(summary = "Update last message timestamp")
    public ResponseEntity<Void> updateLastMessageAt(
            @PathVariable Long id,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime at) {
        roomService.updateLastMessageAt(id, at);
        return ResponseEntity.ok().build();
    }
}