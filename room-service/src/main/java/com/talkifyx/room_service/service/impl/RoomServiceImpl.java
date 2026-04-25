package com.talkifyx.room_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talkifyx.room_service.client.AuthServiceClient;
import com.talkifyx.room_service.client.MessageServiceClient;
import com.talkifyx.room_service.dto.ApiResponse;
import com.talkifyx.room_service.dto.RoomMemberResponse;
import com.talkifyx.room_service.dto.RoomRequest;
import com.talkifyx.room_service.dto.RoomResponse;
import com.talkifyx.room_service.dto.UserDto;
import com.talkifyx.room_service.entity.Room;
import com.talkifyx.room_service.entity.RoomMember;
import com.talkifyx.room_service.exception.RoomException;
import com.talkifyx.room_service.repository.RoomMemberRepository;
import com.talkifyx.room_service.repository.RoomRepository;
import com.talkifyx.room_service.service.RoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final AuthServiceClient authServiceClient;
    private final RoomMemberRepository memberRepository;
    private final MessageServiceClient messageServiceClient;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request, Long createdById) {
        Room room = Room.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(Room.RoomType.valueOf(request.getType()))
                .createdById(createdById)
                .avatarUrl(request.getAvatarUrl())
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
                .maxMembers(request.getMaxMembers())
                .build();
        room = roomRepository.save(room);

        RoomMember admin = RoomMember.builder()
                .roomId(room.getRoomId())
                .userId(createdById)
                .role(RoomMember.MemberRole.ADMIN)
                .build();
        memberRepository.save(admin);

        return mapToResponse(room, createdById);
    }

    @Override
    public RoomResponse getRoomById(Long roomId) {
        Room r = findRoom(roomId);
        return mapToResponse(r, r.getCreatedById());
    }

    @Override
    public List<RoomResponse> getRoomsByUser(Long userId) {
        return roomRepository.findRoomsByUserId(userId)
                .stream().map(r -> mapToResponse(r, userId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomRequest request, Long requesterId) {
        Room room = findRoom(roomId);
        requireAdmin(roomId, requesterId);
        if (request.getName() != null)
            room.setName(request.getName());
        if (request.getDescription() != null)
            room.setDescription(request.getDescription());
        if (request.getAvatarUrl() != null)
            room.setAvatarUrl(request.getAvatarUrl());
        if (request.getIsPrivate() != null)
            room.setIsPrivate(request.getIsPrivate());
        return mapToResponse(roomRepository.save(room), requesterId);
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId, Long requesterId) {
        requireAdmin(roomId, requesterId);
        memberRepository.findByRoomId(roomId).forEach(memberRepository::delete);
        roomRepository.deleteById(roomId);
    }

    @Override
    @Transactional
    public RoomMemberResponse addMember(Long roomId, Long userId, Long requesterId) {
        findRoom(roomId);
        requireAdmin(roomId, requesterId);
        if (memberRepository.findByRoomIdAndUserId(roomId, userId).isPresent())
            throw new RoomException("User already a member");
        RoomMember member = RoomMember.builder()
                .roomId(roomId).userId(userId)
                .role(RoomMember.MemberRole.MEMBER).build();
        return mapMemberToResponse(memberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(Long roomId, Long userId, Long requesterId) {
        requireAdmin(roomId, requesterId);
        memberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    @Override
    public List<RoomMemberResponse> getMembers(Long roomId) {
        return memberRepository.findByRoomId(roomId)
                .stream().map(this::mapMemberToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomMemberResponse updateMemberRole(Long roomId, Long userId, String role, Long requesterId) {
        requireAdmin(roomId, requesterId);
        RoomMember member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RoomException("Member not found"));
        member.setRole(RoomMember.MemberRole.valueOf(role));
        return mapMemberToResponse(memberRepository.save(member));
    }

    @Override
    @Transactional
    public void muteUnmuteMember(Long roomId, Long userId, boolean mute, Long requesterId) {
        requireAdmin(roomId, requesterId);
        RoomMember member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RoomException("Member not found"));
        member.setIsMuted(mute);
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public void updateLastRead(Long roomId, Long userId) {
        memberRepository.findByRoomIdAndUserId(roomId, userId).ifPresent(m -> {
            m.setLastReadAt(LocalDateTime.now());
            memberRepository.save(m);
        });
    }

    @Override
    public long getUnreadCount(Long roomId, Long userId) {
        RoomMember member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RoomException("Member not found"));
        if (member.getLastReadAt() == null)
            return 0;
        try {
            return messageServiceClient.getUnreadCount(roomId, member.getLastReadAt().toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomException("Room not found: " + roomId));
    }

    private void requireAdmin(Long roomId, Long userId) {
        RoomMember member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RoomException("Not a member of this room"));
        if (member.getRole() != RoomMember.MemberRole.ADMIN)
            throw new RoomException("Admin access required");
    }

    private RoomResponse mapToResponse(Room r, Long requestingUserId) {
        RoomResponse.RoomResponseBuilder builder = RoomResponse.builder()
                .roomId(r.getRoomId()).name(r.getName())
                .description(r.getDescription()).type(r.getType().name())
                .createdById(r.getCreatedById()).avatarUrl(r.getAvatarUrl())
                .isPrivate(r.getIsPrivate()).maxMembers(r.getMaxMembers())
                .lastMessageAt(r.getLastMessageAt()).createdAt(r.getCreatedAt())
                .memberCount(memberRepository.countByRoomId(r.getRoomId()));

        if (r.getType() == Room.RoomType.DM) {
            memberRepository.findByRoomId(r.getRoomId()).stream()
                    .filter(m -> !m.getUserId().equals(requestingUserId))
                    .findFirst()
                    .ifPresent(m -> {
                        try {
                            ApiResponse<UserDto> response = authServiceClient.getUserById(m.getUserId());
                            if (response != null && response.getData() != null) {
                                builder.otherUser(response.getData());
                            }
                        } catch (Exception ignored) {
                        }
                    });
        }

        return builder.build();
    }

    private RoomMemberResponse mapMemberToResponse(RoomMember m) {
        return RoomMemberResponse.builder()
                .memberId(m.getMemberId()).roomId(m.getRoomId())
                .userId(m.getUserId()).role(m.getRole().name())
                .joinedAt(m.getJoinedAt()).lastReadAt(m.getLastReadAt())
                .isMuted(m.getIsMuted()).build();
    }

    @Override
    public RoomResponse getRoomById(Long roomId, Long userId) {
        return mapToResponse(findRoom(roomId), userId);
    }
}