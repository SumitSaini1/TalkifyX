package com.talkifyx.presence_service.repository;

import com.talkifyx.presence_service.entity.UserPresence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserPresenceRepositoryTest {

    @Autowired
    UserPresenceRepository repository;

    private UserPresence save(Long userId, String status, String sessionId, LocalDateTime lastPing) {
        return repository.save(UserPresence.builder()
                .userId(userId)
                .status(status)
                .sessionId(sessionId)
                .lastPingAt(lastPing)
                .connectedAt(LocalDateTime.now())
                .build());
    }

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findByUserId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByUserId()")
    class FindByUserId {

        @Test
        @DisplayName("returns presence for existing userId")
        void found() {
            save(1L, "ONLINE", "s1", LocalDateTime.now());

            Optional<UserPresence> result = repository.findByUserId(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo("ONLINE");
        }

        @Test
        @DisplayName("returns empty for unknown userId")
        void notFound() {
            assertThat(repository.findByUserId(999L)).isEmpty();
        }

        @Test
        @DisplayName("userId column is unique — only one record per userId")
        void uniqueConstraint() {
            save(2L, "ONLINE", "s2", LocalDateTime.now());

            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                save(2L, "OFFLINE", "s3", LocalDateTime.now());
                repository.flush();
            });
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findBySessionId
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findBySessionId()")
    class FindBySessionId {

        @Test
        @DisplayName("returns presence for existing sessionId")
        void found() {
            save(10L, "ONLINE", "sess-abc", LocalDateTime.now());

            Optional<UserPresence> result = repository.findBySessionId("sess-abc");

            assertThat(result).isPresent();
            assertThat(result.get().getUserId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("returns empty for unknown sessionId")
        void notFound() {
            assertThat(repository.findBySessionId("nonexistent")).isEmpty();
        }

        @Test
        @DisplayName("returns empty when sessionId is null")
        void nullSession() {
            save(11L, "OFFLINE", null, LocalDateTime.now());

            assertThat(repository.findBySessionId(null)).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findAllByLastPingAtBeforeAndStatusNot
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAllByLastPingAtBeforeAndStatusNot()")
    class FindStale {

        @Test
        @DisplayName("returns only stale non-OFFLINE records")
        void returnsStale() {
            LocalDateTime old = LocalDateTime.now().minusMinutes(5);
            LocalDateTime recent = LocalDateTime.now();

            save(20L, "ONLINE", "s20", old);
            save(21L, "AWAY",   "s21", old);
            save(22L, "ONLINE", "s22", recent);  // not stale
            save(23L, "OFFLINE","s23", old);     // excluded by status

            List<UserPresence> stale = repository
                    .findAllByLastPingAtBeforeAndStatusNot(LocalDateTime.now().minusSeconds(30), "OFFLINE");

            assertThat(stale).hasSize(2);
            assertThat(stale).extracting(UserPresence::getUserId)
                    .containsExactlyInAnyOrder(20L, 21L);
        }

        @Test
        @DisplayName("returns empty when all users are OFFLINE")
        void allOffline() {
            save(30L, "OFFLINE", null, LocalDateTime.now().minusMinutes(10));

            List<UserPresence> stale = repository
                    .findAllByLastPingAtBeforeAndStatusNot(LocalDateTime.now().minusSeconds(30), "OFFLINE");

            assertThat(stale).isEmpty();
        }

        @Test
        @DisplayName("returns empty when no records are stale")
        void noneStale() {
            save(40L, "ONLINE", "s40", LocalDateTime.now());

            List<UserPresence> stale = repository
                    .findAllByLastPingAtBeforeAndStatusNot(LocalDateTime.now().minusSeconds(30), "OFFLINE");

            assertThat(stale).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  findAllByUserIdIn
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAllByUserIdIn()")
    class FindAllByUserIdIn {

        @Test
        @DisplayName("returns records for all matching userIds")
        void found() {
            save(50L, "ONLINE",  "s50", LocalDateTime.now());
            save(51L, "OFFLINE", null,  LocalDateTime.now());
            save(52L, "AWAY",    "s52", LocalDateTime.now());

            List<UserPresence> result = repository.findAllByUserIdIn(List.of(50L, 51L, 52L));

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("ignores unknown userIds in input list")
        void partialMatch() {
            save(60L, "ONLINE", "s60", LocalDateTime.now());

            List<UserPresence> result = repository.findAllByUserIdIn(List.of(60L, 999L));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(60L);
        }

        @Test
        @DisplayName("returns empty for all-unknown userIds")
        void noneFound() {
            List<UserPresence> result = repository.findAllByUserIdIn(List.of(100L, 200L));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for empty input list")
        void emptyInput() {
            List<UserPresence> result = repository.findAllByUserIdIn(List.of());
            assertThat(result).isEmpty();
        }
    }
}
