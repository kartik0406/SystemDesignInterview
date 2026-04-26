package com.sdi.gateway.agent;

import com.sdi.gateway.model.dto.EvaluationResponse;
import com.sdi.gateway.model.enums.CompanyMode;
import com.sdi.gateway.model.enums.DifficultyLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Interview Orchestrator Agent — the brain of the system.
 * Routes requests to specialized agents and manages interview state machine.
 *
 * A2A Communication Flow:
 *   InterviewAgent → QuestionAgent (generate_question)
 *   InterviewAgent → EvaluationAgent (evaluate_answer)
 *   InterviewAgent → HintAgent (provide_hint)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewAgent {

    private final QuestionAgent questionAgent;
    private final EvaluationAgent evaluationAgent;
    private final HintAgent hintAgent;

    /**
     * Route: Generate next interview question.
     * Delegates to QuestionAgent via A2A-style invocation.
     */
    public Map<String, Object> routeToQuestionAgent(UUID sessionId, String topic,
                                                     CompanyMode companyMode,
                                                     DifficultyLevel difficulty,
                                                     List<Map<String, String>> history) {
        log.info("[InterviewAgent] Routing to QuestionAgent for session={}", sessionId);
        return questionAgent.generateQuestion(sessionId, topic, companyMode, difficulty, history);
    }

    /**
     * Route: Evaluate candidate answer.
     * Delegates to EvaluationAgent via A2A-style invocation.
     */
    public EvaluationResponse routeToEvaluationAgent(UUID sessionId, String question,
                                                      String answer, CompanyMode companyMode) {
        log.info("[InterviewAgent] Routing to EvaluationAgent for session={}", sessionId);
        return evaluationAgent.evaluateAnswer(sessionId, question, answer, companyMode);
    }

    /**
     * Route: Generate hint for current question.
     * Delegates to HintAgent via A2A-style invocation.
     */
    public String routeToHintAgent(UUID sessionId, String currentQuestion, int hintLevel) {
        log.info("[InterviewAgent] Routing to HintAgent for session={}, level={}", sessionId, hintLevel);
        return hintAgent.generateHint(sessionId, currentQuestion, hintLevel);
    }

    /**
     * Get all agent cards for A2A discovery.
     */
    public List<AgentCard> discoverAgents() {
        return List.of(
                questionAgent.getCard(),
                evaluationAgent.getCard(),
                hintAgent.getCard()
        );
    }
}
