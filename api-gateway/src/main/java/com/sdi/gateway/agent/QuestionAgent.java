package com.sdi.gateway.agent;

import com.sdi.gateway.client.LlmServiceClient;
import com.sdi.gateway.model.enums.CompanyMode;
import com.sdi.gateway.model.enums.DifficultyLevel;
import com.sdi.gateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Specialized agent for generating adaptive interview questions.
 * Uses RAG context and conversation history to produce relevant follow-ups.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionAgent {

    private final LlmServiceClient llmClient;
    private final SessionService sessionService;

    private final AgentCard card = AgentCard.questionAgent();

    public AgentCard getCard() {
        return card;
    }

    /**
     * Generate the next interview question based on session context.
     *
     * @return Map with keys: "question", "topic_area", "expected_depth"
     */
    public Map<String, Object> generateQuestion(UUID sessionId, String topic,
                                                  CompanyMode companyMode,
                                                  DifficultyLevel difficulty,
                                                  List<Map<String, String>> recentHistory) {
        log.info("[QuestionAgent] Generating question for session={}, topic={}, difficulty={}",
                sessionId, topic, difficulty);

        // Get previous questions to avoid repetition
        List<String> previousQuestions = sessionService.getPreviousQuestions(sessionId);

        // Fetch relevant knowledge via RAG
        List<String> ragContext = llmClient.queryKnowledge(topic, 3);

        // Build request params for LLM service
        Map<String, Object> params = new HashMap<>();
        params.put("topic", topic);
        params.put("company_mode", companyMode.name());
        params.put("difficulty", difficulty.getValue());
        params.put("previous_questions", previousQuestions);
        params.put("conversation_history", recentHistory);
        params.put("rag_context", ragContext);

        // Company-specific focus areas
        List<String> focusAreas = switch (companyMode) {
            case GOOGLE -> List.of("scalability", "distributed systems", "data consistency", "fault tolerance");
            case AMAZON -> List.of("trade-offs", "cost optimization", "customer impact", "operational excellence");
            case GENERAL -> List.of("scalability", "database design", "API design", "trade-offs", "caching");
        };
        params.put("focus_areas", focusAreas);

        Map<String, Object> result = llmClient.generateQuestion(params);

        // Track this question
        String question = (String) result.getOrDefault("question", "Describe the high-level architecture of your system.");
        sessionService.addPreviousQuestion(sessionId, question);
        sessionService.addToHistory(sessionId, "interviewer", question);

        return result;
    }
}
