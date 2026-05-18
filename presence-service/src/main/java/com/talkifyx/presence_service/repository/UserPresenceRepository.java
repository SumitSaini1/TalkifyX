package com.talkifyx.presence_service.repository;

import com.talkifyx.presence_service.entity.UserPresence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPresenceRepository extends JpaRepository<UserPresence, Long> {

    Optional<UserPresence> findByUserId(Long userId);

    Optional<UserPresence> findBySessionId(String sessionId);

    List<UserPresence> findAllByLastPingAtBeforeAndStatusNot(LocalDateTime threshold, String status);

    List<UserPresence> findAllByUserIdIn(List<Long> userIds);
}