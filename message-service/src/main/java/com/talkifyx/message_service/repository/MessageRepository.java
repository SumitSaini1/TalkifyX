package com.talkifyx.message_service.repository;

import com.talkifyx.message_service.entity.DeliveryStatus;
import com.talkifyx.message_service.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.isDeleted = false AND :userId NOT MEMBER OF m.deletedForUsers ORDER BY m.sentAt DESC")
    Page<Message> findVisibleMessagesByRoomId(@Param("roomId") Long roomId, @Param("userId") Long userId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.isDeleted = false AND :userId NOT MEMBER OF m.deletedForUsers ORDER BY m.sentAt DESC")
    List<Message> findVisibleMessagesByRoomIdList(@Param("roomId") Long roomId, @Param("userId") Long userId);

    List<Message> findBySenderId(Long senderId);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.isDeleted = false AND m.sentAt < :before AND :userId NOT MEMBER OF m.deletedForUsers ORDER BY m.sentAt DESC")
    List<Message> findVisibleMessagesByRoomIdAndSentAtBefore(@Param("roomId") Long roomId, @Param("before") LocalDateTime before, @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.isDeleted = false AND m.sentAt > :after AND :userId NOT MEMBER OF m.deletedForUsers ORDER BY m.sentAt ASC")
    List<Message> findVisibleMessagesByRoomIdAndSentAtAfter(@Param("roomId") Long roomId, @Param("after") LocalDateTime after, @Param("userId") Long userId);

    long countByRoomIdAndIsDeletedFalse(Long roomId);

    void deleteByMessageId(String messageId);

    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId AND m.isDeleted = false " +
            "AND :userId NOT MEMBER OF m.deletedForUsers " +
            "AND (LOWER(m.content) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    List<Message> searchInRoom(@Param("roomId") Long roomId, @Param("keyword") String keyword, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomId = :roomId AND m.sentAt > :after AND m.senderId != :userId AND m.isDeleted = false")
    long countUnreadMessages(@Param("roomId") Long roomId, @Param("after") LocalDateTime after,
            @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.deliveryStatus = :status WHERE m.roomId = :roomId AND m.senderId != :readerId AND m.isDeleted = false")
    int bulkUpdateDeliveryStatus(@Param("roomId") Long roomId,
                                  @Param("readerId") Long readerId,
                                  @Param("status") DeliveryStatus status);
}