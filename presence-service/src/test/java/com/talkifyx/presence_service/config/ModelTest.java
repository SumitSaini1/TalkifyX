package com.talkifyx.presence_service.config;

import com.talkifyx.presence_service.dto.PresenceRequest;
import com.talkifyx.presence_service.dto.PresenceResponse;
import com.talkifyx.presence_service.entity.UserPresence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;


class ModelTest {

    
    @Nested
    @DisplayName("UserPresence entity")
    class UserPresenceEntityTests {

        @Test
        @DisplayName("builder sets all fields correctly")
        void builder_allFields() {
            LocalDateTime now = LocalDateTime.now();
            UserPresence p = UserPresence.builder()
                    .presenceId(1L)
                    .userId(42L)
                    .status("ONLINE")
                    .customMessage("Hey")
                    .deviceType("WEB")
                    .ipAddress("10.0.0.1")
                    .sessionId("session_42_001")
                    .connectedAt(now)
                    .lastPingAt(now)
                    .build();

            assertThat(p.getPresenceId()).isEqualTo(1L);
            assertThat(p.getUserId()).isEqualTo(42L);
            assertThat(p.getStatus()).isEqualTo("ONLINE");
            assertThat(p.getCustomMessage()).isEqualTo("Hey");
            assertThat(p.getDeviceType()).isEqualTo("WEB");
            assertThat(p.getIpAddress()).isEqualTo("10.0.0.1");
            assertThat(p.getSessionId()).isEqualTo("session_42_001");
            assertThat(p.getConnectedAt()).isEqualTo(now);
            assertThat(p.getLastPingAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("no-args constructor creates empty entity")
        void noArgsConstructor() {
            UserPresence p = new UserPresence();
            assertThat(p.getPresenceId()).isNull();
            assertThat(p.getUserId()).isNull();
            assertThat(p.getStatus()).isNull();
        }

        @Test
        @DisplayName("setters overwrite builder values")
        void setters() {
            UserPresence p = UserPresence.builder().status("ONLINE").build();
            p.setStatus("OFFLINE");
            p.setSessionId(null);

            assertThat(p.getStatus()).isEqualTo("OFFLINE");
            assertThat(p.getSessionId()).isNull();
        }

        @Test
        @DisplayName("partial builder — only required fields")
        void partialBuilder() {
            UserPresence p = UserPresence.builder().userId(5L).build();
            assertThat(p.getUserId()).isEqualTo(5L);
            assertThat(p.getStatus()).isNull();
            assertThat(p.getSessionId()).isNull();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PresenceRequest DTO
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PresenceRequest DTO")
    class PresenceRequestTests {

        @Test
        @DisplayName("all-args constructor sets all fields")
        void allArgsConstructor() {
            PresenceRequest req = new PresenceRequest(1L, "ONLINE", "Hi", "DESKTOP", "192.168.1.1", "sess-001");

            assertThat(req.getUserId()).isEqualTo(1L);
            assertThat(req.getStatus()).isEqualTo("ONLINE");
            assertThat(req.getCustomMessage()).isEqualTo("Hi");
            assertThat(req.getDeviceType()).isEqualTo("DESKTOP");
            assertThat(req.getIpAddress()).isEqualTo("192.168.1.1");
            assertThat(req.getSessionId()).isEqualTo("sess-001");
        }

        @Test
        @DisplayName("no-args constructor creates empty DTO")
        void noArgsConstructor() {
            PresenceRequest req = new PresenceRequest();
            assertThat(req.getUserId()).isNull();
            assertThat(req.getSessionId()).isNull();
        }

        @Test
        @DisplayName("setters work correctly")
        void setters() {
            PresenceRequest req = new PresenceRequest();
            req.setUserId(99L);
            req.setStatus("AWAY");
            req.setDeviceType("IOS");

            assertThat(req.getUserId()).isEqualTo(99L);
            assertThat(req.getStatus()).isEqualTo("AWAY");
            assertThat(req.getDeviceType()).isEqualTo("IOS");
        }

        @Test
        @DisplayName("builder sets all fields")
        void builder() {
            PresenceRequest req = PresenceRequest.builder()
                    .userId(5L)
                    .status("BUSY")
                    .sessionId("s5")
                    .build();

            assertThat(req.getUserId()).isEqualTo(5L);
            assertThat(req.getStatus()).isEqualTo("BUSY");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PresenceResponse DTO
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PresenceResponse DTO")
    class PresenceResponseTests {

        @Test
        @DisplayName("builder sets all fields correctly")
        void builder_allFields() {
            LocalDateTime now = LocalDateTime.now();
            PresenceResponse resp = PresenceResponse.builder()
                    .presenceId(1L)
                    .userId(10L)
                    .status("ONLINE")
                    .customMessage("msg")
                    .deviceType("WEB")
                    .sessionId("sess-10")
                    .connectedAt(now)
                    .lastPingAt(now)
                    .build();

            assertThat(resp.getPresenceId()).isEqualTo(1L);
            assertThat(resp.getUserId()).isEqualTo(10L);
            assertThat(resp.getStatus()).isEqualTo("ONLINE");
            assertThat(resp.getCustomMessage()).isEqualTo("msg");
            assertThat(resp.getDeviceType()).isEqualTo("WEB");
            assertThat(resp.getSessionId()).isEqualTo("sess-10");
            assertThat(resp.getConnectedAt()).isEqualTo(now);
            assertThat(resp.getLastPingAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("no-args constructor returns all nulls")
        void noArgs() {
            PresenceResponse resp = new PresenceResponse();
            assertThat(resp.getUserId()).isNull();
            assertThat(resp.getStatus()).isNull();
        }

        @Test
        @DisplayName("setters mutate correctly")
        void setters() {
            PresenceResponse resp = PresenceResponse.builder().status("ONLINE").build();
            resp.setStatus("OFFLINE");
            resp.setSessionId(null);
            assertThat(resp.getStatus()).isEqualTo("OFFLINE");
            assertThat(resp.getSessionId()).isNull();
        }

        @Test
        @DisplayName("partial builder — only status and userId")
        void partialBuilder() {
            PresenceResponse resp = PresenceResponse.builder()
                    .userId(99L).status("OFFLINE").build();
            assertThat(resp.getUserId()).isEqualTo(99L);
            assertThat(resp.getPresenceId()).isNull();
            assertThat(resp.getSessionId()).isNull();
        }
    }
}
