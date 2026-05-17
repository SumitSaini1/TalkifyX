package com.talkifyx.presence_service.service;

import com.talkifyx.presence_service.client.ChatNotifyClient;
import com.talkifyx.presence_service.dto.PresenceRequest;
import com.talkifyx.presence_service.dto.PresenceResponse;
import com.talkifyx.presence_service.entity.UserPresence;
import com.talkifyx.presence_service.repository.UserPresenceRepository;
import com.talkifyx.presence_service.service.impl.PresenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceImplTest {

    @Mock UserPresenceRepository repository;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;
    @Mock ChatNotifyClient chatNotifyClient;

    @InjectMocks PresenceServiceImpl service;

    

    private UserPresence buildPresence(Long userId, String status) {
        return UserPresence.builder()
                .presenceId(1L)
                .userId(userId)
                .status(status)
                .sessionId("session_" + userId + "_123")
                .connectedAt(LocalDateTime.now())
                .lastPingAt(LocalDateTime.now())
                .build();
    }

    private PresenceRequest buildRequest(Long userId) {
        PresenceRequest req = new PresenceRequest();
        req.setUserId(userId);
        req.setStatus("ONLINE");
        req.setCustomMessage("Hey!");
        req.setDeviceType("MOBILE");
        req.setIpAddress("127.0.0.1");
        req.setSessionId("session_" + userId + "_123");
        return req;
    }

    @BeforeEach
    void setupRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  connect()
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("connect()")
    class ConnectTests {

        @Test
        @DisplayName("creates new presence when user not found in DB")
        void connect_newUser_createsPresence() {
            PresenceRequest req = buildRequest(10L);
            when(repository.findByUserId(10L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> {
                UserPresence p = inv.getArgument(0);
                p.setPresenceId(1L);
                return p;
            });

            PresenceResponse resp = service.connect(req);

            assertThat(resp.getUserId()).isEqualTo(10L);
            assertThat(resp.getStatus()).isEqualTo("ONLINE");
            assertThat(resp.getCustomMessage()).isEqualTo("Hey!");
            assertThat(resp.getDeviceType()).isEqualTo("MOBILE");
            assertThat(resp.getSessionId()).isEqualTo("session_10_123");
        }

        @Test
        @DisplayName("updates existing presence when user already exists")
        void connect_existingUser_updatesPresence() {
            PresenceRequest req = buildRequest(10L);
            UserPresence existing = buildPresence(10L, "OFFLINE");
            when(repository.findByUserId(10L)).thenReturn(Optional.of(existing));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PresenceResponse resp = service.connect(req);

            assertThat(resp.getStatus()).isEqualTo("ONLINE");
        }

        @Test
        @DisplayName("caches result in Redis with correct key")
        void connect_cachesInRedis() {
            PresenceRequest req = buildRequest(10L);
            when(repository.findByUserId(10L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> {
                UserPresence p = inv.getArgument(0);
                p.setPresenceId(1L);
                return p;
            });

            service.connect(req);

            verify(valueOps).set(eq("presence:10"), any(PresenceResponse.class));
        }

        @Test
        @DisplayName("notifies chat-service after connect")
        void connect_notifiesChatService() {
            PresenceRequest req = buildRequest(10L);
            when(repository.findByUserId(10L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> {
                UserPresence p = inv.getArgument(0);
                p.setPresenceId(1L);
                return p;
            });

            service.connect(req);

            verify(chatNotifyClient).notifyPresence(any(PresenceResponse.class));
        }

        @Test
        @DisplayName("sets connectedAt and lastPingAt to now")
        void connect_setsTimestamps() {
            PresenceRequest req = buildRequest(10L);
            when(repository.findByUserId(10L)).thenReturn(Optional.empty());

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.connect(req);

            UserPresence saved = captor.getValue();
            assertThat(saved.getConnectedAt()).isNotNull();
            assertThat(saved.getLastPingAt()).isNotNull();
        }

        @Test
        @DisplayName("does not throw when chat-service notify fails")
        void connect_chatNotifyFails_noException() {
            PresenceRequest req = buildRequest(10L);
            when(repository.findByUserId(10L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> {
                UserPresence p = inv.getArgument(0);
                p.setPresenceId(1L);
                return p;
            });
            doThrow(new RuntimeException("feign error")).when(chatNotifyClient).notifyPresence(any());

            assertThatCode(() -> service.connect(req)).doesNotThrowAnyException();
        }
    }

    

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("updates status and customMessage successfully")
        void updateStatus_success() {
            UserPresence p = buildPresence(5L, "ONLINE");
            when(repository.findByUserId(5L)).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PresenceResponse resp = service.updateStatus(5L, "AWAY", "Lunch break");

            assertThat(resp.getStatus()).isEqualTo("AWAY");
            assertThat(resp.getCustomMessage()).isEqualTo("Lunch break");
        }

        @Test
        @DisplayName("throws RuntimeException when userId not found")
        void updateStatus_notFound_throws() {
            when(repository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStatus(99L, "AWAY", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("caches updated presence in Redis")
        void updateStatus_cachesInRedis() {
            UserPresence p = buildPresence(5L, "ONLINE");
            when(repository.findByUserId(5L)).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(5L, "BUSY", null);

            verify(valueOps).set(eq("presence:5"), any(PresenceResponse.class));
        }

        @Test
        @DisplayName("notifies chat-service after status update")
        void updateStatus_notifiesChatService() {
            UserPresence p = buildPresence(5L, "ONLINE");
            when(repository.findByUserId(5L)).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateStatus(5L, "BUSY", "In meeting");

            verify(chatNotifyClient).notifyPresence(any(PresenceResponse.class));
        }

        @Test
        @DisplayName("accepts null customMessage without error")
        void updateStatus_nullCustomMessage_ok() {
            UserPresence p = buildPresence(5L, "ONLINE");
            when(repository.findByUserId(5L)).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.updateStatus(5L, "AWAY", null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("does not throw when chat notify fails during updateStatus")
        void updateStatus_chatNotifyFails_swallowed() {
            UserPresence p = buildPresence(5L, "ONLINE");
            when(repository.findByUserId(5L)).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("timeout")).when(chatNotifyClient).notifyPresence(any());

            assertThatCode(() -> service.updateStatus(5L, "AWAY", null)).doesNotThrowAnyException();
        }
    }

   

    @Nested
    @DisplayName("ping()")
    class PingTests {

        @Test
        @DisplayName("normal ping updates lastPingAt and returns ONLINE")
        void ping_found_updatesLastPing() {
            UserPresence p = buildPresence(7L, "ONLINE");
            when(repository.findBySessionId("session_7_123")).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PresenceResponse resp = service.ping("session_7_123");

            assertThat(resp.getStatus()).isEqualTo("ONLINE");
            verify(repository).save(any());
        }

        @Test
        @DisplayName("auto-heal: creates presence from valid session format when not in DB")
        void ping_autoHeal_validSessionFormat() {
            when(repository.findBySessionId("session_7_999")).thenReturn(Optional.empty());
            when(repository.findByUserId(7L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> {
                UserPresence p = inv.getArgument(0);
                p.setPresenceId(1L);
                return p;
            });

            PresenceResponse resp = service.ping("session_7_999");

            assertThat(resp.getStatus()).isEqualTo("ONLINE");
            assertThat(resp.getUserId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("auto-heal: updates existing user when session not found but user exists")
        void ping_autoHeal_existingUserReused() {
            UserPresence existing = buildPresence(7L, "OFFLINE");
            when(repository.findBySessionId("session_7_999")).thenReturn(Optional.empty());
            when(repository.findByUserId(7L)).thenReturn(Optional.of(existing));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PresenceResponse resp = service.ping("session_7_999");

            assertThat(resp.getStatus()).isEqualTo("ONLINE");
        }

        @Test
        @DisplayName("returns OFFLINE response for invalid session format (no underscore)")
        void ping_invalidFormat_returnsOffline() {
            when(repository.findBySessionId("invalidsession")).thenReturn(Optional.empty());

            PresenceResponse resp = service.ping("invalidsession");

            assertThat(resp.getStatus()).isEqualTo("OFFLINE");
            assertThat(resp.getSessionId()).isEqualTo("invalidsession");
        }

        @Test
        @DisplayName("returns OFFLINE for session with non-numeric userId segment")
        void ping_nonNumericUserId_returnsOffline() {
            when(repository.findBySessionId("session_abc_123")).thenReturn(Optional.empty());

            PresenceResponse resp = service.ping("session_abc_123");

            assertThat(resp.getStatus()).isEqualTo("OFFLINE");
        }

        @Test
        @DisplayName("caches presence in Redis after successful ping")
        void ping_cachesInRedis() {
            UserPresence p = buildPresence(7L, "ONLINE");
            when(repository.findBySessionId("session_7_123")).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.ping("session_7_123");

            verify(valueOps).set(eq("presence:7"), any(PresenceResponse.class));
        }

        @Test
        @DisplayName("notifies chat-service after successful ping")
        void ping_notifiesChatService() {
            UserPresence p = buildPresence(7L, "ONLINE");
            when(repository.findBySessionId("session_7_123")).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.ping("session_7_123");

            verify(chatNotifyClient).notifyPresence(any(PresenceResponse.class));
        }

        @Test
        @DisplayName("does NOT notify chat-service when returning OFFLINE (invalid session)")
        void ping_invalidSession_doesNotNotify() {
            when(repository.findBySessionId("bad")).thenReturn(Optional.empty());

            service.ping("bad");

            verify(chatNotifyClient, never()).notifyPresence(any());
        }
    }

    
    @Nested
    @DisplayName("disconnect()")
    class DisconnectTests {

        @Test
        @DisplayName("sets status OFFLINE, clears sessionId, deletes Redis key")
        void disconnect_setsOfflineAndClearsSession() {
            UserPresence p = buildPresence(8L, "ONLINE");
            when(repository.findBySessionId("session_8_123")).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.disconnect("session_8_123");

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("OFFLINE");
            assertThat(captor.getValue().getSessionId()).isNull();
        }

        @Test
        @DisplayName("deletes Redis key on disconnect")
        void disconnect_deletesRedisKey() {
            UserPresence p = buildPresence(8L, "ONLINE");
            when(repository.findBySessionId("session_8_123")).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.disconnect("session_8_123");

            verify(redisTemplate).delete("presence:8");
        }

        @Test
        @DisplayName("notifies chat-service on disconnect")
        void disconnect_notifiesChatService() {
            UserPresence p = buildPresence(8L, "ONLINE");
            when(repository.findBySessionId("session_8_123")).thenReturn(Optional.of(p));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.disconnect("session_8_123");

            verify(chatNotifyClient).notifyPresence(any(PresenceResponse.class));
        }

        @Test
        @DisplayName("does nothing when session not found")
        void disconnect_sessionNotFound_noOp() {
            when(repository.findBySessionId("unknown")).thenReturn(Optional.empty());

            service.disconnect("unknown");

            verify(repository, never()).save(any());
            verify(chatNotifyClient, never()).notifyPresence(any());
        }
    }

    

    @Nested
    @DisplayName("getByUserId()")
    class GetByUserIdTests {

        @Test
        @DisplayName("returns cached response from Redis when available")
        void getByUserId_returnsCachedValue() {
            PresenceResponse cached = PresenceResponse.builder().userId(3L).status("ONLINE").build();
            when(valueOps.get("presence:3")).thenReturn(cached);

            PresenceResponse resp = service.getByUserId(3L);

            assertThat(resp.getStatus()).isEqualTo("ONLINE");
            verify(repository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("falls back to DB when Redis cache miss")
        void getByUserId_cacheMiss_fetchesFromDB() {
            when(valueOps.get("presence:3")).thenReturn(null);
            UserPresence p = buildPresence(3L, "AWAY");
            when(repository.findByUserId(3L)).thenReturn(Optional.of(p));

            PresenceResponse resp = service.getByUserId(3L);

            assertThat(resp.getStatus()).isEqualTo("AWAY");
        }

        @Test
        @DisplayName("returns OFFLINE when not in Redis and not in DB")
        void getByUserId_notFound_returnsOffline() {
            when(valueOps.get("presence:99")).thenReturn(null);
            when(repository.findByUserId(99L)).thenReturn(Optional.empty());

            PresenceResponse resp = service.getByUserId(99L);

            assertThat(resp.getUserId()).isEqualTo(99L);
            assertThat(resp.getStatus()).isEqualTo("OFFLINE");
        }
    }

    
    @Nested
    @DisplayName("getBulk()")
    class GetBulkTests {

        @Test
        @DisplayName("returns presence list for given user IDs")
        void getBulk_returnsList() {
            List<UserPresence> presences = List.of(
                    buildPresence(1L, "ONLINE"),
                    buildPresence(2L, "OFFLINE")
            );
            when(repository.findAllByUserIdIn(List.of(1L, 2L))).thenReturn(presences);

            List<PresenceResponse> result = service.getBulk(List.of(1L, 2L));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(PresenceResponse::getUserId).containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("returns empty list when no users found")
        void getBulk_noResults_returnsEmpty() {
            when(repository.findAllByUserIdIn(List.of(100L, 200L))).thenReturn(List.of());

            List<PresenceResponse> result = service.getBulk(List.of(100L, 200L));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("maps all fields correctly from entity to response")
        void getBulk_mapsFieldsCorrectly() {
            UserPresence p = buildPresence(1L, "ONLINE");
            p.setCustomMessage("Hey");
            p.setDeviceType("WEB");
            when(repository.findAllByUserIdIn(List.of(1L))).thenReturn(List.of(p));

            PresenceResponse resp = service.getBulk(List.of(1L)).get(0);

            assertThat(resp.getCustomMessage()).isEqualTo("Hey");
            assertThat(resp.getDeviceType()).isEqualTo("WEB");
        }
    }

    
    @Nested
    @DisplayName("cleanStaleSessions()")
    class CleanStaleSessionsTests {

        @Test
        @DisplayName("marks stale ONLINE users as OFFLINE")
        void cleanStale_setsOffline() {
            UserPresence stale1 = buildPresence(11L, "ONLINE");
            UserPresence stale2 = buildPresence(12L, "AWAY");
            when(repository.findAllByLastPingAtBeforeAndStatusNot(any(), eq("OFFLINE")))
                    .thenReturn(List.of(stale1, stale2));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.cleanStaleSessions();

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(repository, times(2)).save(captor.capture());
            captor.getAllValues().forEach(p -> assertThat(p.getStatus()).isEqualTo("OFFLINE"));
        }

        @Test
        @DisplayName("clears sessionId for stale users")
        void cleanStale_clearsSessionId() {
            UserPresence stale = buildPresence(11L, "ONLINE");
            when(repository.findAllByLastPingAtBeforeAndStatusNot(any(), eq("OFFLINE")))
                    .thenReturn(List.of(stale));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.cleanStaleSessions();

            ArgumentCaptor<UserPresence> captor = ArgumentCaptor.forClass(UserPresence.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getSessionId()).isNull();
        }

        @Test
        @DisplayName("deletes Redis key for each stale user")
        void cleanStale_deletesRedisKeys() {
            UserPresence stale1 = buildPresence(11L, "ONLINE");
            UserPresence stale2 = buildPresence(12L, "ONLINE");
            stale2.setUserId(12L);
            when(repository.findAllByLastPingAtBeforeAndStatusNot(any(), eq("OFFLINE")))
                    .thenReturn(List.of(stale1, stale2));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.cleanStaleSessions();

            verify(redisTemplate).delete("presence:11");
            verify(redisTemplate).delete("presence:12");
        }

        @Test
        @DisplayName("notifies chat-service for each stale user")
        void cleanStale_notifiesChatService() {
            UserPresence stale1 = buildPresence(11L, "ONLINE");
            UserPresence stale2 = buildPresence(12L, "ONLINE");
            stale2.setUserId(12L);
            when(repository.findAllByLastPingAtBeforeAndStatusNot(any(), eq("OFFLINE")))
                    .thenReturn(List.of(stale1, stale2));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.cleanStaleSessions();

            verify(chatNotifyClient, times(2)).notifyPresence(any());
        }

        @Test
        @DisplayName("does nothing when no stale sessions")
        void cleanStale_noStale_noOps() {
            when(repository.findAllByLastPingAtBeforeAndStatusNot(any(), eq("OFFLINE")))
                    .thenReturn(List.of());

            service.cleanStaleSessions();

            verify(repository, never()).save(any());
            verify(chatNotifyClient, never()).notifyPresence(any());
        }

        @Test
        @DisplayName("query threshold is approximately 60 seconds ago")
        void cleanStale_usesCorrectThreshold() {
            when(repository.findAllByLastPingAtBeforeAndStatusNot(any(), eq("OFFLINE")))
                    .thenReturn(List.of());

            LocalDateTime before = LocalDateTime.now().minusSeconds(61);
            service.cleanStaleSessions();
            LocalDateTime after = LocalDateTime.now().minusSeconds(59);

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(repository).findAllByLastPingAtBeforeAndStatusNot(captor.capture(), eq("OFFLINE"));

            LocalDateTime threshold = captor.getValue();
            assertThat(threshold).isBetween(before, after);
        }
    }
}
