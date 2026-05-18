package com.talkifyx.presence_service.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.talkifyx.presence_service.dto.PresenceRequest;
import com.talkifyx.presence_service.dto.PresenceResponse;
import com.talkifyx.presence_service.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PresenceResourceTest {

    @Mock PresenceService presenceService;
    @InjectMocks PresenceResource presenceResource;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(presenceResource).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private PresenceResponse sampleResponse(Long userId) {
        return PresenceResponse.builder()
                .presenceId(1L)
                .userId(userId)
                .status("ONLINE")
                .customMessage("Hello")
                .deviceType("MOBILE")
                .sessionId("session_" + userId + "_123")
                .connectedAt(LocalDateTime.now())
                .lastPingAt(LocalDateTime.now())
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  POST /api/presence/connect
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/presence/connect")
    class ConnectEndpoint {

        @Test
        @DisplayName("200 OK with body on valid request")
        void connect_returns200() throws Exception {
            PresenceRequest req = new PresenceRequest();
            req.setUserId(1L);
            req.setSessionId("session_1_123");

            when(presenceService.connect(any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(post("/api/presence/connect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.status").value("ONLINE"));
        }

        @Test
        @DisplayName("response contains all expected fields")
        void connect_responseFields() throws Exception {
            PresenceRequest req = new PresenceRequest();
            req.setUserId(1L);
            when(presenceService.connect(any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(post("/api/presence/connect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(jsonPath("$.presenceId").value(1))
                    .andExpect(jsonPath("$.customMessage").value("Hello"))
                    .andExpect(jsonPath("$.deviceType").value("MOBILE"))
                    .andExpect(jsonPath("$.sessionId").value("session_1_123"));
        }

        @Test
        @DisplayName("calls service.connect exactly once")
        void connect_callsServiceOnce() throws Exception {
            PresenceRequest req = new PresenceRequest();
            req.setUserId(1L);
            when(presenceService.connect(any())).thenReturn(sampleResponse(1L));

            mockMvc.perform(post("/api/presence/connect")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));

            verify(presenceService, times(1)).connect(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PUT /api/presence/{userId}/status
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/presence/{userId}/status")
    class UpdateStatusEndpoint {

        @Test
        @DisplayName("200 OK with updated status")
        void updateStatus_returns200() throws Exception {
            PresenceResponse resp = sampleResponse(5L);
            resp.setStatus("AWAY");
            when(presenceService.updateStatus(eq(5L), eq("AWAY"), eq("Lunch")))
                    .thenReturn(resp);

            mockMvc.perform(put("/api/presence/5/status")
                            .param("status", "AWAY")
                            .param("customMessage", "Lunch"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AWAY"));
        }

        @Test
        @DisplayName("works without optional customMessage param")
        void updateStatus_noCustomMessage() throws Exception {
            when(presenceService.updateStatus(eq(5L), eq("BUSY"), isNull()))
                    .thenReturn(sampleResponse(5L));

            mockMvc.perform(put("/api/presence/5/status")
                            .param("status", "BUSY"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("passes correct userId from path variable")
        void updateStatus_pathVariable() throws Exception {
            when(presenceService.updateStatus(eq(42L), any(), any()))
                    .thenReturn(sampleResponse(42L));

            mockMvc.perform(put("/api/presence/42/status").param("status", "ONLINE"))
                    .andExpect(jsonPath("$.userId").value(42));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  POST /api/presence/ping
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/presence/ping")
    class PingEndpoint {

        @Test
        @DisplayName("200 OK with ONLINE response")
        void ping_returns200() throws Exception {
            when(presenceService.ping("session_1_123")).thenReturn(sampleResponse(1L));

            mockMvc.perform(post("/api/presence/ping")
                            .param("sessionId", "session_1_123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ONLINE"));
        }

        @Test
        @DisplayName("returns OFFLINE response for invalid session")
        void ping_offline_response() throws Exception {
            PresenceResponse offline = PresenceResponse.builder()
                    .sessionId("bad").status("OFFLINE").build();
            when(presenceService.ping("bad")).thenReturn(offline);

            mockMvc.perform(post("/api/presence/ping").param("sessionId", "bad"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("OFFLINE"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  POST /api/presence/disconnect
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/presence/disconnect")
    class DisconnectEndpoint {

        @Test
        @DisplayName("204 No Content on disconnect")
        void disconnect_returns204() throws Exception {
            doNothing().when(presenceService).disconnect("session_1_123");

            mockMvc.perform(post("/api/presence/disconnect")
                            .param("sessionId", "session_1_123"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("calls service.disconnect with correct sessionId")
        void disconnect_callsService() throws Exception {
            mockMvc.perform(post("/api/presence/disconnect")
                    .param("sessionId", "session_99_456"));

            verify(presenceService).disconnect("session_99_456");
        }

        @Test
        @DisplayName("no response body on disconnect")
        void disconnect_emptyBody() throws Exception {
            mockMvc.perform(post("/api/presence/disconnect")
                            .param("sessionId", "session_1_123"))
                    .andExpect(content().string(""));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GET /api/presence/{userId}
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/presence/{userId}")
    class GetByUserIdEndpoint {

        @Test
        @DisplayName("200 OK with presence data")
        void getByUserId_returns200() throws Exception {
            when(presenceService.getByUserId(3L)).thenReturn(sampleResponse(3L));

            mockMvc.perform(get("/api/presence/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(3));
        }

        @Test
        @DisplayName("returns OFFLINE status for unknown user")
        void getByUserId_offline() throws Exception {
            when(presenceService.getByUserId(999L)).thenReturn(
                    PresenceResponse.builder().userId(999L).status("OFFLINE").build());

            mockMvc.perform(get("/api/presence/999"))
                    .andExpect(jsonPath("$.status").value("OFFLINE"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  POST /api/presence/bulk
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/presence/bulk")
    class GetBulkEndpoint {

        @Test
        @DisplayName("200 OK with list of presence responses")
        void getBulk_returns200() throws Exception {
            List<PresenceResponse> list = List.of(sampleResponse(1L), sampleResponse(2L));
            when(presenceService.getBulk(List.of(1L, 2L))).thenReturn(list);

            mockMvc.perform(post("/api/presence/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[1, 2]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].userId").value(1))
                    .andExpect(jsonPath("$[1].userId").value(2));
        }

        @Test
        @DisplayName("returns empty array for empty input")
        void getBulk_emptyList() throws Exception {
            when(presenceService.getBulk(List.of())).thenReturn(List.of());

            mockMvc.perform(post("/api/presence/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("calls service with correct list of userIds")
        void getBulk_passesCorrectIds() throws Exception {
            when(presenceService.getBulk(List.of(10L, 20L, 30L))).thenReturn(List.of());

            mockMvc.perform(post("/api/presence/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("[10, 20, 30]"));

            verify(presenceService).getBulk(List.of(10L, 20L, 30L));
        }
    }
}
