package com.talkifyx.room_service.service;

import com.talkifyx.room_service.dto.*;
import java.time.LocalDateTime;
import java.util.List;

public interface RoomService {
    RoomResponse createRoom(RoomRequest request, Long createdById);
    RoomResponse getRoomById(Long roomId);
    List<RoomResponse> getRoomsByUser(Long userId);
    RoomResponse updateRoom(Long roomId, RoomRequest request, Long requesterId);
    void deleteRoom(Long roomId, Long requesterId);
    RoomResponse getRoomById(Long roomId, Long userId);
    RoomMemberResponse addMember(Long roomId, Long userId, Long requesterId);
    void removeMember(Long roomId, Long userId, Long requesterId);
    List<RoomMemberResponse> getMembers(Long roomId);
    RoomMemberResponse updateMemberRole(Long roomId, Long userId, String role, Long requesterId);
    void muteUnmuteMember(Long roomId, Long userId, boolean mute, Long requesterId);
    void updateLastRead(Long roomId, Long userId);
    long getUnreadCount(Long roomId, Long userId);
    void updateLastMessageAt(Long roomId, LocalDateTime at);
}