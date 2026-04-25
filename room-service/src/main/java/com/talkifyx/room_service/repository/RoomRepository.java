package com.talkifyx.room_service.repository;

import com.talkifyx.room_service.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCreatedById(Long createdById);
    List<Room> findByType(Room.RoomType type);

    @Query("SELECT r FROM Room r JOIN RoomMember m ON r.roomId = m.roomId WHERE m.userId = :userId")
    List<Room> findRoomsByUserId(@Param("userId") Long userId);
}