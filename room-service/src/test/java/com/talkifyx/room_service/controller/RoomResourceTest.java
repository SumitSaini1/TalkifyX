package com.talkifyx.room_service.controller;

import com.talkifyx.room_service.dto.*;
import com.talkifyx.room_service.exception.RoomException;
import com.talkifyx.room_service.resource.RoomResource;
import com.talkifyx.room_service.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomResourceTest {

    @Mock private RoomService roomService;
    @InjectMocks private RoomResource roomResource;

    private RoomResponse testRoomResponse;
    private RoomMemberResponse testMemberResponse;

    @BeforeEach
    void setUp() {
        testRoomResponse = RoomResponse.builder()
                .roomId(1L).name("Test Room").type("GROUP").createdById(10L).build();

        testMemberResponse = RoomMemberResponse.builder()
                .memberId(1L).roomId(1L).userId(20L).role("MEMBER").build();
    }

    @Test
    void createRoom_Returns200WithRoomResponse() {
        RoomRequest request = RoomRequest.builder().name("Test Room").type("GROUP").build();
        when(roomService.createRoom(request, 10L)).thenReturn(testRoomResponse);

        ResponseEntity<RoomResponse> response = roomResource.createRoom(request, 10L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Test Room", response.getBody().getName());
    }

    @Test
    void getRoomById_Returns200() {
        when(roomService.getRoomById(1L, 10L)).thenReturn(testRoomResponse);

        ResponseEntity<RoomResponse> response = roomResource.getRoomById(1L, 10L);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void getRoomById_NotFound_ThrowsException() {
        when(roomService.getRoomById(99L, 10L)).thenThrow(new RoomException("Room not found: 99"));

        assertThrows(RoomException.class, () -> roomResource.getRoomById(99L, 10L));
    }

    @Test
    void getRoomsByUser_ReturnsList() {
        when(roomService.getRoomsByUser(10L)).thenReturn(List.of(testRoomResponse));

        ResponseEntity<List<RoomResponse>> response = roomResource.getRoomsByUser(10L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void updateRoom_AdminSuccess_Returns200() {
        RoomRequest request = RoomRequest.builder().name("Updated").type("GROUP").build();
        when(roomService.updateRoom(1L, request, 10L)).thenReturn(testRoomResponse);

        ResponseEntity<RoomResponse> response = roomResource.updateRoom(1L, request, 10L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void deleteRoom_AdminSuccess_Returns204() {
        doNothing().when(roomService).deleteRoom(1L, 10L);

        ResponseEntity<Void> response = roomResource.deleteRoom(1L, 10L);

        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void addMember_Success_Returns200() {
        when(roomService.addMember(1L, 20L, 10L)).thenReturn(testMemberResponse);

        ResponseEntity<RoomMemberResponse> response = roomResource.addMember(1L, 20L, 10L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(20L, response.getBody().getUserId());
    }

    @Test
    void removeMember_Success_Returns204() {
        doNothing().when(roomService).removeMember(1L, 20L, 10L);

        ResponseEntity<Void> response = roomResource.removeMember(1L, 20L, 10L);

        assertEquals(204, response.getStatusCodeValue());
    }

    @Test
    void getMembers_ReturnsList() {
        when(roomService.getMembers(1L)).thenReturn(List.of(testMemberResponse));

        ResponseEntity<List<RoomMemberResponse>> response = roomResource.getMembers(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void updateRole_Success_Returns200() {
        when(roomService.updateMemberRole(1L, 20L, "ADMIN", 10L)).thenReturn(testMemberResponse);

        ResponseEntity<RoomMemberResponse> response = roomResource.updateRole(1L, 20L, "ADMIN", 10L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void muteUnmute_Success_Returns200() {
        doNothing().when(roomService).muteUnmuteMember(1L, 20L, true, 10L);

        ResponseEntity<Void> response = roomResource.muteUnmute(1L, 20L, true, 10L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void getUnreadCount_Returns200WithCount() {
        when(roomService.getUnreadCount(1L, 20L)).thenReturn(5L);

        ResponseEntity<Long> response = roomResource.getUnreadCount(1L, 20L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(5L, response.getBody());
    }

    @Test
    void updateLastRead_Returns200() {
        doNothing().when(roomService).updateLastRead(1L, 10L);

        ResponseEntity<Void> response = roomResource.updateLastRead(1L, 10L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void updateLastMessageAt_Returns200() {
        LocalDateTime now = LocalDateTime.now();
        doNothing().when(roomService).updateLastMessageAt(1L, now);

        ResponseEntity<Void> response = roomResource.updateLastMessageAt(1L, now);

        assertEquals(200, response.getStatusCodeValue());
    }
}
