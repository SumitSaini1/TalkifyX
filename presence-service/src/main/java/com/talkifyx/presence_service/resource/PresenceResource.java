package com.talkifyx.presence_service.resource;

import com.talkifyx.presence_service.dto.PresenceRequest;
import com.talkifyx.presence_service.dto.PresenceResponse;
import com.talkifyx.presence_service.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
@Tag(name = "Presence", description = "User presence management")
public class PresenceResource {

    private final PresenceService presenceService;

    @PostMapping("/connect")
    @Operation(summary = "Mark user as connected/online")
    public ResponseEntity<PresenceResponse> connect(@RequestBody PresenceRequest request) {
        return ResponseEntity.ok(presenceService.connect(request));
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "Update user status")
    public ResponseEntity<PresenceResponse> updateStatus(
            @PathVariable Long userId,
            @RequestParam String status,
            @RequestParam(required = false) String customMessage) {
        return ResponseEntity.ok(presenceService.updateStatus(userId, status, customMessage));
    }

    @PostMapping("/ping")
    @Operation(summary = "Ping to keep session alive")
    public ResponseEntity<PresenceResponse> ping(@RequestParam String sessionId) {
        return ResponseEntity.ok(presenceService.ping(sessionId));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Mark user as disconnected/offline")
    public ResponseEntity<Void> disconnect(@RequestParam String sessionId) {
        presenceService.disconnect(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get presence by userId")
    public ResponseEntity<PresenceResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(presenceService.getByUserId(userId));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Get presence for multiple users")
    public ResponseEntity<List<PresenceResponse>> getBulk(@RequestBody List<Long> userIds) {
        return ResponseEntity.ok(presenceService.getBulk(userIds));
    }
}