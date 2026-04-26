package com.sdi.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdi.gateway.agent.InterviewAgent;
import com.sdi.gateway.client.LlmServiceClient;
import com.sdi.gateway.config.AppConfig;
import com.sdi.gateway.exception.InterviewException;
import com.sdi.gateway.model.dto.*;
import com.sdi.gateway.model.entity.InterviewRound;
import com.sdi.gateway.model.entity.InterviewSession;
import com.sdi.gateway.model.enums.DifficultyLevel;
import com.sdi.gateway.model.enums.SessionStatus;
import com.sdi.gateway.repository.InterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core interview orchestration service.
 * Manages the interview lifecycle: start → answer → evaluate → next question → report.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewAgent interviewAgent;
    private final LlmServiceClient llmClient;
    private final SessionService sessionService;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    // ─── Available Topics ────────────────────────────────────

    public static final List<Map<String, String>> AVAILABLE_TOPICS = List.of(
            Map.of("id", "url-shortener", "name", "Design URL Shortener", "description", "Design a URL shortening service like bit.ly"),
            Map.of("id", "twitter", "name", "Design Twitter", "description", "Design a social media feed system"),
            Map.of("id", "netflix", "name", "Design Netflix", "description", "Design a video streaming platform"),
            Map.of("id", "uber", "name", "Design Uber", "description", "Design a ride-sharing service"),
            Map.of("id", "whatsapp", "name", "Design WhatsApp", "description", "Design a real-time messaging system"),
            Map.of("id", "instagram", "name", "Design Instagram", "description", "Design a photo-sharing social network"),
            Map.of("id", "rate-limiter", "name", "Design Rate Limiter", "description", "Design a distributed rate limiting system"),
            Map.of("id", "notification", "name", "Design Notification System", "description", "Design a scalable notification service"),
            Map.of("id", "search-engine", "name", "Design Search Engine", "description", "Design a web search engine like Google"),
            Map.of("id", "payment", "name", "Design Payment System", "description", "Design a payment processing platform")
    );

    // ─── Start Interview ─────────────────────────────────────

    @Transactional
    public InterviewResponse startInterview(StartInterviewRequest request) {
        log.info("Starting interview: topic={}, company={}", request.getTopic(), request.getCompanyMode());

        // Create session entity
        InterviewSession session = InterviewSession.builder()
                .topic(request.getTopic())
                .companyMode(request.getCompanyMode())
                .currentDifficulty(DifficultyLevel.MEDIUM)
                .currentRound(1)
                .maxRounds(appConfig.getMaxRounds())
                .status(SessionStatus.IN_PROGRESS)
                .build();

        session = sessionRepository.save(session);

        // Store session metadata in Redis
        sessionService.setSessionMeta(session.getId(), "topic", request.getTopic());
        sessionService.setSessionMeta(session.getId(), "companyMode", request.getCompanyMode().name());

        // Generate first question via QuestionAgent
        Map<String, Object> questionResult = interviewAgent.routeToQuestionAgent(
                session.getId(),
                request.getTopic(),
                request.getCompanyMode(),
                DifficultyLevel.MEDIUM,
                List.of()
        );

        String question = (String) questionResult.getOrDefault("question",
                "Let's start with the high-level design. How would you approach " + request.getTopic() + "?");
        String topicArea = (String) questionResult.getOrDefault("topic_area", "high-level design");

        // Create first round
        InterviewRound round = InterviewRound.builder()
                .roundNumber(1)
                .question(question)
                .difficulty(DifficultyLevel.MEDIUM)
                .topicArea(topicArea)
                .build();

        session.addRound(round);
        sessionRepository.save(session);

        return InterviewResponse.builder()
                .sessionId(session.getId())
                .topic(session.getTopic())
                .companyMode(session.getCompanyMode())
                .currentRound(1)
                .maxRounds(session.getMaxRounds())
                .difficulty(DifficultyLevel.MEDIUM)
                .status(SessionStatus.IN_PROGRESS)
                .question(question)
                .topicArea(topicArea)
                .isLastRound(false)
                .build();
    }

    // ─── Submit Answer ───────────────────────────────────────

    @Transactional
    public InterviewResponse submitAnswer(SubmitAnswerRequest request) {
        InterviewSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new InterviewException("Session not found: " + request.getSessionId()));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new InterviewException("Interview session is not in progress");
        }

        // Find current round
        InterviewRound currentRound = session.getRounds().stream()
                .filter(r -> r.getRoundNumber() == session.getCurrentRound())
                .findFirst()
                .orElseThrow(() -> new InterviewException("Current round not found"));

        // Store user answer
        currentRound.setUserAnswer(request.getAnswer());
        currentRound.setAnsweredAt(LocalDateTime.now());

        // Evaluate via EvaluationAgent
        EvaluationResponse evaluation = interviewAgent.routeToEvaluationAgent(
                session.getId(),
                currentRound.getQuestion(),
                request.getAnswer(),
                session.getCompanyMode()
        );

        currentRound.setScore(evaluation.getScore());
        try {
            currentRound.setEvaluation(objectMapper.writeValueAsString(evaluation));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize evaluation", e);
        }

        // Adjust difficulty based on score
        DifficultyLevel newDifficulty = DifficultyLevel.adjust(
                session.getCurrentDifficulty(), evaluation.getScore());
        session.setCurrentDifficulty(newDifficulty);

        boolean isLastRound = session.getCurrentRound() >= session.getMaxRounds();

        if (isLastRound) {
            // Complete the interview
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);

            return InterviewResponse.builder()
                    .sessionId(session.getId())
                    .topic(session.getTopic())
                    .companyMode(session.getCompanyMode())
                    .currentRound(session.getCurrentRound())
                    .maxRounds(session.getMaxRounds())
                    .difficulty(newDifficulty)
                    .status(SessionStatus.COMPLETED)
                    .evaluation(evaluation)
                    .isLastRound(true)
                    .build();
        }

        // Generate next question
        session.setCurrentRound(session.getCurrentRound() + 1);

        List<Map<String, String>> recentHistory = sessionService.getRecentHistory(session.getId(), 3);

        Map<String, Object> nextQuestionResult = interviewAgent.routeToQuestionAgent(
                session.getId(),
                session.getTopic(),
                session.getCompanyMode(),
                newDifficulty,
                recentHistory
        );

        String nextQuestion = (String) nextQuestionResult.getOrDefault("question",
                "Can you elaborate on your design choices?");
        String nextTopicArea = (String) nextQuestionResult.getOrDefault("topic_area", "deep-dive");

        // Create next round
        InterviewRound nextRound = InterviewRound.builder()
                .roundNumber(session.getCurrentRound())
                .question(nextQuestion)
                .difficulty(newDifficulty)
                .topicArea(nextTopicArea)
                .build();

        session.addRound(nextRound);
        sessionRepository.save(session);

        return InterviewResponse.builder()
                .sessionId(session.getId())
                .topic(session.getTopic())
                .companyMode(session.getCompanyMode())
                .currentRound(session.getCurrentRound())
                .maxRounds(session.getMaxRounds())
                .difficulty(newDifficulty)
                .status(SessionStatus.IN_PROGRESS)
                .question(nextQuestion)
                .topicArea(nextTopicArea)
                .evaluation(evaluation)
                .isLastRound(session.getCurrentRound() >= session.getMaxRounds())
                .build();
    }

    // ─── Get Session State ───────────────────────────────────

    public InterviewResponse getSession(UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new InterviewException("Session not found: " + sessionId));

        InterviewRound latestRound = session.getRounds().stream()
                .max(Comparator.comparingInt(InterviewRound::getRoundNumber))
                .orElse(null);

        return InterviewResponse.builder()
                .sessionId(session.getId())
                .topic(session.getTopic())
                .companyMode(session.getCompanyMode())
                .currentRound(session.getCurrentRound())
                .maxRounds(session.getMaxRounds())
                .difficulty(session.getCurrentDifficulty())
                .status(session.getStatus())
                .question(latestRound != null ? latestRound.getQuestion() : null)
                .topicArea(latestRound != null ? latestRound.getTopicArea() : null)
                .isLastRound(session.getCurrentRound() >= session.getMaxRounds())
                .build();
    }

    // ─── Generate Final Report ───────────────────────────────

    @Transactional(readOnly = true)
    public FinalReportResponse generateReport(UUID sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new InterviewException("Session not found: " + sessionId));

        List<FinalReportResponse.RoundSummary> roundSummaries = new ArrayList<>();
        List<String> allStrengths = new ArrayList<>();
        List<String> allWeaknesses = new ArrayList<>();
        Map<String, List<Double>> rubricAccumulator = new HashMap<>();

        for (InterviewRound round : session.getRounds()) {
            FinalReportResponse.RoundSummary.RoundSummaryBuilder summaryBuilder =
                    FinalReportResponse.RoundSummary.builder()
                            .roundNumber(round.getRoundNumber())
                            .question(round.getQuestion())
                            .answer(round.getUserAnswer())
                            .score(round.getScore() != null ? round.getScore() : 0)
                            .difficulty(round.getDifficulty() != null ? round.getDifficulty().name() : "MEDIUM");

            if (round.getEvaluation() != null) {
                try {
                    EvaluationResponse eval = objectMapper.readValue(
                            round.getEvaluation(), EvaluationResponse.class);

                    summaryBuilder.strengths(eval.getStrengths());
                    summaryBuilder.weaknesses(eval.getWeaknesses());

                    if (eval.getStrengths() != null) allStrengths.addAll(eval.getStrengths());
                    if (eval.getWeaknesses() != null) allWeaknesses.addAll(eval.getWeaknesses());

                    if (eval.getRubricBreakdown() != null) {
                        eval.getRubricBreakdown().forEach((key, value) ->
                                rubricAccumulator.computeIfAbsent(key, k -> new ArrayList<>()).add(value));
                    }
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse evaluation for round {}", round.getRoundNumber());
                }
            }

            roundSummaries.add(summaryBuilder.build());
        }

        // Calculate overall score
        double overallScore = session.getRounds().stream()
                .filter(r -> r.getScore() != null)
                .mapToDouble(InterviewRound::getScore)
                .average()
                .orElse(0.0);

        // Aggregate rubric scores
        Map<String, Double> aggregatedRubric = rubricAccumulator.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0)));

        // Deduplicate strengths/weaknesses
        List<String> uniqueStrengths = allStrengths.stream().distinct().limit(5).toList();
        List<String> uniqueWeaknesses = allWeaknesses.stream().distinct().limit(5).toList();

        // Generate improvement suggestions based on weaknesses
        List<String> suggestions = generateSuggestions(uniqueWeaknesses);

        // Generate architecture diagram
        String diagram = llmClient.generateDiagram(session.getTopic());

        return FinalReportResponse.builder()
                .sessionId(session.getId())
                .topic(session.getTopic())
                .companyMode(session.getCompanyMode().name())
                .overallScore(Math.round(overallScore * 10.0) / 10.0)
                .totalRounds(roundSummaries.size())
                .rounds(roundSummaries)
                .overallStrengths(uniqueStrengths)
                .overallWeaknesses(uniqueWeaknesses)
                .improvementSuggestions(suggestions)
                .aggregatedRubric(aggregatedRubric)
                .architectureDiagram(diagram)
                .build();
    }

    // ─── Request Hint ────────────────────────────────────────

    public String requestHint(HintRequest request) {
        InterviewSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new InterviewException("Session not found: " + request.getSessionId()));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new InterviewException("Interview is not in progress");
        }

        InterviewRound currentRound = session.getRounds().stream()
                .filter(r -> r.getRoundNumber() == session.getCurrentRound())
                .findFirst()
                .orElseThrow(() -> new InterviewException("Current round not found"));

        return interviewAgent.routeToHintAgent(
                session.getId(), currentRound.getQuestion(), request.getHintLevel());
    }

    // ─── Get Available Topics ────────────────────────────────

    public List<Map<String, String>> getTopics() {
        return AVAILABLE_TOPICS;
    }

    // ─── Helpers ─────────────────────────────────────────────

    private List<String> generateSuggestions(List<String> weaknesses) {
        List<String> suggestions = new ArrayList<>();
        for (String weakness : weaknesses) {
            String lower = weakness.toLowerCase();
            if (lower.contains("shard") || lower.contains("partition")) {
                suggestions.add("Study database sharding and partitioning strategies (consistent hashing, range-based, hash-based)");
            }
            if (lower.contains("consisten") || lower.contains("cap")) {
                suggestions.add("Deep dive into CAP theorem and consistency models (eventual, strong, causal)");
            }
            if (lower.contains("cache") || lower.contains("caching")) {
                suggestions.add("Explore caching patterns: write-through, write-behind, cache-aside, and invalidation strategies");
            }
            if (lower.contains("api") || lower.contains("interface")) {
                suggestions.add("Practice designing clean REST APIs with proper resource modeling and versioning");
            }
            if (lower.contains("scale") || lower.contains("scalab")) {
                suggestions.add("Study horizontal vs vertical scaling, load balancing, and auto-scaling patterns");
            }
            if (lower.contains("trade") || lower.contains("tradeoff")) {
                suggestions.add("Practice articulating trade-offs: latency vs throughput, consistency vs availability, cost vs performance");
            }
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Review system design fundamentals: scalability, reliability, and maintainability");
            suggestions.add("Practice drawing architecture diagrams and explaining component interactions");
        }
        return suggestions.stream().distinct().toList();
    }
}
