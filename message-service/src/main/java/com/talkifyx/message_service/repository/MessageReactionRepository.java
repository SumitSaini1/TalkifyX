package com.talkifyx.message_service.repository;

import com.talkifyx.message_service.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    List<MessageReaction> findByMessageId(String messageId);

    Optional<MessageReaction> findByMessageIdAndUserId(String messageId, Long userId);

    @Query("SELECT r FROM MessageReaction r WHERE r.messageId IN :messageIds")
    List<MessageReaction> findByMessageIdIn(@Param("messageIds") List<String> messageIds);

    void deleteByMessageIdAndUserId(String messageId, Long userId);
}
