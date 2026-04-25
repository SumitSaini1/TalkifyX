package com.talkifyx.room_service.repository;

import com.talkifyx.room_service.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    List<RoomMember> findByRoomId(Long roomId);
    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);
    long countByRoomId(Long roomId);
    void deleteByRoomIdAndUserId(Long roomId, Long userId);
}