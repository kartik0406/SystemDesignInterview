package com.sdi.gateway.agent;

import com.sdi.gateway.client.LlmServiceClient;
import com.sdi.gateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Specialized agent for providing progressive hints.
 * Gives partial guidance without revealing full solutions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HintAgent {

    private final LlmServiceClient llmClient;
    private final SessionService sessionService;

    private final AgentCard card = AgentCard.hintAgent();

    public AgentCard getCard() {
        return card;
    }

    /**
     * Generate a hint at the specified level.
     * Level 1: Nudge (general direction)
     * Level 2: Direction (specific area to think about)
     * Level 3: Partial solution (concrete starting point)
     */
    public String generateHint(UUID sessionId, String currentQuestion, int hintLevel) {
        log.info("[HintAgent] Generating hint level={} for session={}", hintLevel, sessionId);

        // Get conversation context
        List<Map<String, String>> history = sessionService.getRecentHistory(sessionId, 2);

        // Fetch relevant knowledge
        List<String> ragContext = llmClient.queryKnowledge(currentQuestion, 3);

        Map<String, Object> params = new HashMap<>();
        params.put("question", currentQuestion);
        params.put("hint_level", hintLevel);
        params.put("conversation_history", history);
        params.put("rag_context", ragContext);

        return llmClient.generateHint(params);
    }
}
