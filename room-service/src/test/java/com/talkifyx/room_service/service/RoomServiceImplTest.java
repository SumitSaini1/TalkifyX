package com.talkifyx.room_service.service;

import com.talkifyx.room_service.client.AuthServiceClient;
import com.talkifyx.room_service.client.ChatNotifyClient;
import com.talkifyx.room_service.client.MessageServiceClient;
import com.talkifyx.room_service.dto.*;
import com.talkifyx.room_service.entity.Room;
import com.talkifyx.room_service.entity.RoomMember;
import com.talkifyx.room_service.exception.RoomException;
import com.talkifyx.room_service.repository.RoomMemberRepository;
import com.talkifyx.room_service.repository.RoomRepository;
import com.talkifyx.room_service.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomServiceImplTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomMemberRepository memberRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private MessageServiceClient messageServiceClient;
    @Mock private ChatNotifyClient chatNotifyClient;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room testRoom;
    private RoomMember adminMember;
    private RoomMember regularMember;
    private RoomRequest groupRequest;

    @BeforeEach
    void setUp() {
        testRoom = Room.builder()
                .roomId(1L).name("Test Room")
                .description("desc").type(Room.RoomType.GROUP)
                .createdById(10L).isPrivate(false).build();

        adminMember = RoomMember.builder()
                .memberId(1L).roomId(1L).userId(10L)
                .role(RoomMember.MemberRole.ADMIN).isMuted(false).build();

        regularMember = RoomMember.builder()
                .memberId(2L).roomId(1L).userId(20L)
                .role(RoomMember.MemberRole.MEMBER).isMuted(false).build();

        groupRequest = RoomRequest.builder()
                .name("Test Room").description("desc")
                .type("GROUP").isPrivate(false).build();
    }

    // ===================== CREATE ROOM =====================

    @Test
    void createRoom_Success_ReturnsRoomResponse() {
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(memberRepository.save(any(RoomMember.class))).thenReturn(adminMember);
        when(memberRepository.findByRoomId(1L)).thenReturn(List.of(adminMember));
        when(memberRepository.countByRoomId(1L)).thenReturn(1L);
        doNothing().when(chatNotifyClient).notifyNewRoom(any(), any());

        RoomResponse response = roomService.createRoom(groupRequest, 10L);

        assertNotNull(response);
        assertEquals("Test Room", response.getName());
        assertEquals(10L, response.getCreatedById());
        verify(roomRepository).save(any(Room.class));
        verify(memberRepository).save(any(RoomMember.class)); // creator added as ADMIN
    }

    @Test
    void createRoom_NotifyFails_StillReturnsResponse() {
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(memberRepository.save(any(RoomMember.class))).thenReturn(adminMember);
        when(memberRepository.findByRoomId(1L)).thenReturn(List.of(adminMember));
        when(memberRepository.countByRoomId(1L)).thenReturn(1L);
        doThrow(new RuntimeException("WebSocket down"))
                .when(chatNotifyClient).notifyNewRoom(any(), any());

        // Should NOT throw even if notification fails
        assertDoesNotThrow(() -> roomService.createRoom(groupRequest, 10L));
    }

    // ===================== GET ROOM =====================

    @Test
    void getRoomById_Found_ReturnsResponse() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findByRoomId(1L)).thenReturn(List.of(adminMember));
        when(memberRepository.countByRoomId(1L)).thenReturn(1L);

        RoomResponse response = roomService.getRoomById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getRoomId());
    }

    @Test
    void getRoomById_NotFound_ThrowsRoomException() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RoomException.class, () -> roomService.getRoomById(99L));
    }

    @Test
    void getRoomsByUser_ReturnsList() {
        when(roomRepository.findRoomsByUserId(10L)).thenReturn(List.of(testRoom));
        when(memberRepository.findByRoomId(1L)).thenReturn(List.of(adminMember));
        when(memberRepository.countByRoomId(1L)).thenReturn(1L);

        List<RoomResponse> rooms = roomService.getRoomsByUser(10L);

        assertFalse(rooms.isEmpty());
        assertEquals(1, rooms.size());
    }

    // ===================== UPDATE ROOM =====================

    @Test
    void updateRoom_AdminSuccess_UpdatesFields() {
        RoomRequest updateReq = RoomRequest.builder().name("Updated Name").build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(memberRepository.findByRoomId(1L)).thenReturn(List.of(adminMember));
        when(memberRepository.countByRoomId(1L)).thenReturn(1L);

        RoomResponse response = roomService.updateRoom(1L, updateReq, 10L);

        assertNotNull(response);
        verify(roomRepository).save(testRoom);
    }

    @Test
    void updateRoom_NonAdminRequester_ThrowsRoomException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findByRoomIdAndUserId(1L, 20L)).thenReturn(Optional.of(regularMember));

        assertThrows(RoomException.class,
                () -> roomService.updateRoom(1L, groupRequest, 20L));
    }

    // ===================== DELETE ROOM =====================

    @Test
    void deleteRoom_AdminSuccess_DeletesRoomAndMembers() {
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByRoomId(1L)).thenReturn(List.of(adminMember, regularMember));
        doNothing().when(memberRepository).delete(any());
        doNothing().when(roomRepository).deleteById(1L);

        roomService.deleteRoom(1L, 10L);

        verify(roomRepository).deleteById(1L);
        verify(memberRepository, times(2)).delete(any(RoomMember.class));
    }

    @Test
    void deleteRoom_NotMember_ThrowsRoomException() {
        when(memberRepository.findByRoomIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(RoomException.class, () -> roomService.deleteRoom(1L, 99L));
    }

    // ===================== ADD MEMBER =====================

    @Test
    void addMember_Success_ReturnsMemberResponse() {
        RoomMember newMember = RoomMember.builder()
                .memberId(3L).roomId(1L).userId(30L)
                .role(RoomMember.MemberRole.MEMBER).isMuted(false).build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByRoomIdAndUserId(1L, 30L)).thenReturn(Optional.empty());
        when(memberRepository.save(any(RoomMember.class))).thenReturn(newMember);

        RoomMemberResponse response = roomService.addMember(1L, 30L, 10L);

        assertNotNull(response);
        assertEquals(30L, response.getUserId());
    }

    @Test
    void addMember_AlreadyMember_ThrowsRoomException() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByRoomIdAndUserId(1L, 20L)).thenReturn(Optional.of(regularMember));

        assertThrows(RoomException.class, () -> roomService.addMember(1L, 20L, 10L));
    }

    // ===================== REMOVE MEMBER =====================

    @Test
    void removeMember_AdminSuccess_DeletesMember() {
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        doNothing().when(memberRepository).deleteByRoomIdAndUserId(1L, 20L);

        roomService.removeMember(1L, 20L, 10L);

        verify(memberRepository).deleteByRoomIdAndUserId(1L, 20L);
    }

    // ===================== GET MEMBERS =====================

    @Test
    void getMembers_ReturnsList() {
        when(memberRepository.findByRoomId(1L)).thenReturn(List.of(adminMember, regularMember));

        List<RoomMemberResponse> members = roomService.getMembers(1L);

        assertEquals(2, members.size());
    }

    // ===================== UPDATE MEMBER ROLE =====================

    @Test
    void updateMemberRole_Success_UpdatesRole() {
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByRoomIdAndUserId(1L, 20L)).thenReturn(Optional.of(regularMember));
        when(memberRepository.save(any(RoomMember.class))).thenReturn(regularMember);

        RoomMemberResponse response = roomService.updateMemberRole(1L, 20L, "ADMIN", 10L);

        assertNotNull(response);
        verify(memberRepository).save(regularMember);
    }

    @Test
    void updateMemberRole_MemberNotFound_ThrowsException() {
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByRoomIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(RoomException.class,
                () -> roomService.updateMemberRole(1L, 99L, "ADMIN", 10L));
    }

    // ===================== MUTE/UNMUTE =====================

    @Test
    void muteUnmuteMember_Mute_SetsMutedTrue() {
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByRoomIdAndUserId(1L, 20L)).thenReturn(Optional.of(regularMember));
        when(memberRepository.save(any(RoomMember.class))).thenReturn(regularMember);

        roomService.muteUnmuteMember(1L, 20L, true, 10L);

        assertTrue(regularMember.getIsMuted());
        verify(memberRepository).save(regularMember);
    }

    @Test
    void muteUnmuteMember_MemberNotFound_ThrowsException() {
        when(memberRepository.findByRoomIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByRoomIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(RoomException.class,
                () -> roomService.muteUnmuteMember(1L, 99L, true, 10L));
    }

    // ===================== LAST READ / UNREAD COUNT =====================

    @Test
    void updateLastRead_MemberExists_UpdatesTime() {
        when(memberRepository.findByRoomIdAndUserId(1L, 20L)).thenReturn(Optional.of(regularMember));
        when(memberRepository.save(any(RoomMember.class))).thenReturn(regularMember);

        roomService.updateLastRead(1L, 20L);

        assertNotNull(regularMember.getLastReadAt());
        verify(memberRepository).save(regularMember);
    }

    @Test
    void getUnreadCount_NullLastReadAt_ReturnsZero() {
        regularMember.setLastReadAt(null);
        when(memberRepository.findByRoomIdAndUserId(1L, 20L)).thenReturn(Optional.of(regularMember));

        long count = roomService.getUnreadCount(1L, 20L);

        assertEquals(0, count);
    }

    @Test
    void getUnreadCount_MessageClientFails_ReturnsZero() {
        regularMember.setLastReadAt(LocalDateTime.now().minusHours(1));
        when(memberRepository.findByRoomIdAndUserId(1L, 20L)).thenReturn(Optional.of(regularMember));
        when(messageServiceClient.getUnreadCount(anyLong(), anyString()))
                .thenThrow(new RuntimeException("Service unavailable"));

        long count = roomService.getUnreadCount(1L, 20L);

        assertEquals(0, count);
    }

    @Test
    void getUnreadCount_MemberNotFound_ThrowsException() {
        when(memberRepository.findByRoomIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(RoomException.class, () -> roomService.getUnreadCount(1L, 99L));
    }

    // ===================== UPDATE LAST MESSAGE AT =====================

    @Test
    void updateLastMessageAt_RoomExists_UpdatesTime() {
        LocalDateTime now = LocalDateTime.now();
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        roomService.updateLastMessageAt(1L, now);

        assertEquals(now, testRoom.getLastMessageAt());
        verify(roomRepository).save(testRoom);
    }

    @Test
    void updateLastMessageAt_RoomNotFound_DoesNothing() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> roomService.updateLastMessageAt(99L, LocalDateTime.now()));
        verify(roomRepository, never()).save(any());
    }

    // ===================== EMPTY RESULTS =====================

    @Test
    void getRoomsByUser_NoRooms_ReturnsEmptyList() {
        when(roomRepository.findRoomsByUserId(10L)).thenReturn(Collections.emptyList());

        List<RoomResponse> rooms = roomService.getRoomsByUser(10L);

        assertTrue(rooms.isEmpty());
    }

    @Test
    void getMembers_NoMembers_ReturnsEmptyList() {
        when(memberRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());

        List<RoomMemberResponse> members = roomService.getMembers(1L);

        assertTrue(members.isEmpty());
    }
}
