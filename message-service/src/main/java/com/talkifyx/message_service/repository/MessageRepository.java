package com.talkifyx.message_service.repository;

import com.talkifyx.message_service.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    Page<Message> findByRoomIdAndIsDeletedFalseOrderBySentAtDesc(Long roomId, Pageable pageable);

    List<Message> findByRoomIdOrderBySentAtDesc(Long roomId);

    List<Message> findBySenderId(Long senderId);

    List<Message> findByRoomIdAndSentAtBefore(Long roomId, LocalDateTime before);

    List<Message> findByRoomIdAndSentAtAfter(Long roomId, LocalDateTime after);

    long countByRoomIdAndIsDeletedFalse(Long roomId);

    void deleteByMessageId(String messageId);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.isDeleted = false " +
            "AND (LOWER(m.content) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    List<Message> searchInRoom(@Param("roomId") Long roomId, @Param("keyword") String keyword);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomId = :roomId AND m.sentAt > :after AND m.senderId != :userId AND m.isDeleted = false")
    long countUnreadMessages(@Param("roomId") Long roomId, @Param("after") LocalDateTime after,
            @Param("userId") Long userId);
}