package com.sdi.gateway.agent;

import com.sdi.gateway.client.LlmServiceClient;
import com.sdi.gateway.model.dto.EvaluationResponse;
import com.sdi.gateway.model.enums.CompanyMode;
import com.sdi.gateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Specialized agent for evaluating candidate answers.
 * Uses RAG context for reference architectures and structured rubric scoring.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluationAgent {

    private final LlmServiceClient llmClient;
    private final SessionService sessionService;

    private final AgentCard card = AgentCard.evaluationAgent();

    public AgentCard getCard() {
        return card;
    }

    /**
     * Evaluate a candidate's answer using RAG-enhanced rubric scoring.
     */
    public EvaluationResponse evaluateAnswer(UUID sessionId, String question,
                                              String answer, CompanyMode companyMode) {
        log.info("[EvaluationAgent] Evaluating answer for session={}", sessionId);

        // Fetch reference context via RAG
        List<String> ragContext = llmClient.queryKnowledge(question + " " + answer, 5);

        // Get conversation history for context
        List<Map<String, String>> history = sessionService.getRecentHistory(sessionId, 3);

        // Build evaluation request
        Map<String, Object> params = new HashMap<>();
        params.put("question", question);
        params.put("answer", answer);
        params.put("company_mode", companyMode.name());
        params.put("rag_context", ragContext);
        params.put("conversation_history", history);

        // Company-specific rubric weights
        Map<String, Double> rubricWeights = switch (companyMode) {
            case GOOGLE -> Map.of(
                    "scalability", 2.5, "database_design", 2.0,
                    "api_design", 1.5, "tradeoffs", 2.0, "clarity", 2.0);
            case AMAZON -> Map.of(
                    "scalability", 2.0, "database_design", 1.5,
                    "api_design", 2.0, "tradeoffs", 2.5, "clarity", 2.0);
            case GENERAL -> Map.of(
                    "scalability", 2.0, "database_design", 2.0,
                    "api_design", 2.0, "tradeoffs", 2.0, "clarity", 2.0);
        };
        params.put("rubric_weights", rubricWeights);

        // Record user answer in session history
        sessionService.addToHistory(sessionId, "candidate", answer);

        return llmClient.evaluateAnswer(params);
    }
}
