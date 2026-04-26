package com.sdi.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Redis-backed session memory service.
 * Stores conversation history, context, and interview state for follow-up generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "sdi:session:";
    private static final String CONTEXT_PREFIX = "sdi:context:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    // ─── Conversation History ────────────────────────────────

    public void addToHistory(UUID sessionId, String role, String content) {
        String key = SESSION_PREFIX + sessionId + ":history";
        Map<String, String> entry = Map.of("role", role, "content", content, 
                                            "timestamp", String.valueOf(System.currentTimeMillis()));
        try {
            String json = objectMapper.writeValueAsString(entry);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, SESSION_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize history entry: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getHistory(UUID sessionId) {
        String key = SESSION_PREFIX + sessionId + ":history";
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return List.of();

        List<Map<String, String>> history = new ArrayList<>();
        for (Object item : raw) {
            try {
                history.add(objectMapper.readValue(item.toString(), Map.class));
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize history entry: {}", e.getMessage());
            }
        }
        return history;
    }

    /**
     * Get last N exchanges for context window (to avoid token overflow).
     */
    public List<Map<String, String>> getRecentHistory(UUID sessionId, int lastN) {
        String key = SESSION_PREFIX + sessionId + ":history";
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) return List.of();

        long start = Math.max(0, size - lastN * 2); // each exchange is 2 entries (Q + A)
        List<Object> raw = redisTemplate.opsForList().range(key, start, -1);
        if (raw == null) return List.of();

        List<Map<String, String>> history = new ArrayList<>();
        for (Object item : raw) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> entry = objectMapper.readValue(item.toString(), Map.class);
                history.add(entry);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize: {}", e.getMessage());
            }
        }
        return history;
    }

    // ─── Previous Questions Tracking ────────────────────────

    public void addPreviousQuestion(UUID sessionId, String question) {
        String key = SESSION_PREFIX + sessionId + ":questions";
        redisTemplate.opsForList().rightPush(key, question);
        redisTemplate.expire(key, SESSION_TTL);
    }

    public List<String> getPreviousQuestions(UUID sessionId) {
        String key = SESSION_PREFIX + sessionId + ":questions";
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return List.of();
        return raw.stream().map(Object::toString).toList();
    }

    // ─── Session Metadata ────────────────────────────────────

    public void setSessionMeta(UUID sessionId, String field, String value) {
        String key = SESSION_PREFIX + sessionId + ":meta";
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, SESSION_TTL);
    }

    public String getSessionMeta(UUID sessionId, String field) {
        String key = SESSION_PREFIX + sessionId + ":meta";
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    // ─── Cleanup ─────────────────────────────────────────────

    public void clearSession(UUID sessionId) {
        String pattern = SESSION_PREFIX + sessionId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
