package com.talkifyx.presence_service.service;

import com.talkifyx.presence_service.dto.PresenceRequest;
import com.talkifyx.presence_service.dto.PresenceResponse;
import java.util.List;

public interface PresenceService {
    PresenceResponse connect(PresenceRequest request);
    PresenceResponse updateStatus(Long userId, String status, String customMessage);
    PresenceResponse ping(String sessionId);
    void disconnect(String sessionId);
    PresenceResponse getByUserId(Long userId);
    List<PresenceResponse> getBulk(List<Long> userIds);
    void cleanStaleSessions();
}