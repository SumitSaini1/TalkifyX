package com.talkifyx.presence_service.service.impl;

import com.talkifyx.presence_service.dto.PresenceRequest;
import com.talkifyx.presence_service.dto.PresenceResponse;
import com.talkifyx.presence_service.entity.UserPresence;
import com.talkifyx.presence_service.repository.UserPresenceRepository;
import com.talkifyx.presence_service.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {

    private final UserPresenceRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_PREFIX = "presence:";

    @Override
    public PresenceResponse connect(PresenceRequest req) {
        UserPresence presence = repository.findByUserId(req.getUserId())
                .orElse(UserPresence.builder().userId(req.getUserId()).build());
        presence.setStatus("ONLINE");
        presence.setCustomMessage(req.getCustomMessage());
        presence.setDeviceType(req.getDeviceType());
        presence.setIpAddress(req.getIpAddress());
        presence.setSessionId(req.getSessionId());
        presence.setConnectedAt(LocalDateTime.now());
        presence.setLastPingAt(LocalDateTime.now());
        presence = repository.save(presence);
        cacheToRedis(presence);
        return toResponse(presence);
    }

    @Override
    public PresenceResponse updateStatus(Long userId, String status, String customMessage) {
        UserPresence presence = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Presence not found for userId: " + userId));
        presence.setStatus(status);
        presence.setCustomMessage(customMessage);
        presence = repository.save(presence);
        cacheToRedis(presence);
        return toResponse(presence);
    }

    @Override
    public PresenceResponse ping(String sessionId) {
        UserPresence presence = repository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        presence.setLastPingAt(LocalDateTime.now());
        presence = repository.save(presence);
        cacheToRedis(presence);
        return toResponse(presence);
    }

    @Override
    public void disconnect(String sessionId) {
        repository.findBySessionId(sessionId).ifPresent(presence -> {
            presence.setStatus("OFFLINE");
            presence.setSessionId(null);
            repository.save(presence);
            redisTemplate.delete(REDIS_PREFIX + presence.getUserId());
        });
    }

    @Override
    public PresenceResponse getByUserId(Long userId) {
        Object cached = redisTemplate.opsForValue().get(REDIS_PREFIX + userId);
        if (cached != null) {
            return (PresenceResponse) cached;
        }
        UserPresence presence = repository.findByUserId(userId)
                .orElse(UserPresence.builder().userId(userId).status("OFFLINE").build());
        return toResponse(presence);
    }

    @Override
    public List<PresenceResponse> getBulk(List<Long> userIds) {
        return repository.findAllByUserIdIn(userIds)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    public void cleanStaleSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(60);
        List<UserPresence> stale = repository.findAllByLastPingAtBeforeAndStatusNot(threshold, "OFFLINE");
        stale.forEach(presence -> {
            presence.setStatus("OFFLINE");
            presence.setSessionId(null);
            repository.save(presence);
            redisTemplate.delete(REDIS_PREFIX + presence.getUserId());
        });
    }

    private void cacheToRedis(UserPresence presence) {
        redisTemplate.opsForValue().set(REDIS_PREFIX + presence.getUserId(), toResponse(presence));
    }

    private PresenceResponse toResponse(UserPresence p) {
        return PresenceResponse.builder()
                .presenceId(p.getPresenceId())
                .userId(p.getUserId())
                .status(p.getStatus())
                .customMessage(p.getCustomMessage())
                .deviceType(p.getDeviceType())
                .sessionId(p.getSessionId())
                .connectedAt(p.getConnectedAt())
                .lastPingAt(p.getLastPingAt())
                .build();
    }
}