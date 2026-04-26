package com.sdi.gateway.client;

import com.sdi.gateway.config.AppConfig;
import com.sdi.gateway.model.dto.EvaluationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * HTTP client for communicating with the Python LLM/MCP service.
 * Invokes MCP tools: RAG, Scoring, Diagram, Hint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmServiceClient {

    private final RestTemplate restTemplate;
    private final AppConfig appConfig;

    // ─── RAG Tool ────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<String> queryKnowledge(String query, int topK) {
        String url = appConfig.getLlmServiceUrl() + "/tools/rag/query";
        Map<String, Object> body = Map.of("query", query, "top_k", topK);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<String>) response.getBody().get("chunks");
            }
        } catch (Exception e) {
            log.error("RAG query failed: {}", e.getMessage());
        }
        return List.of();
    }

    // ─── Question Generation ────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> generateQuestion(Map<String, Object> params) {
        String url = appConfig.getLlmServiceUrl() + "/tools/generate-question";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, params, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Question generation failed: {}", e.getMessage());
        }
        return Map.of("question", "Tell me about how you would approach designing this system at a high level.",
                       "topic_area", "high-level design",
                       "expected_depth", "overview");
    }

    // ─── Evaluation ──────────────────────────────────────────

    public EvaluationResponse evaluateAnswer(Map<String, Object> params) {
        String url = appConfig.getLlmServiceUrl() + "/tools/score";

        try {
            ResponseEntity<EvaluationResponse> response = restTemplate.postForEntity(
                    url, params, EvaluationResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Evaluation failed: {}", e.getMessage());
        }
        return EvaluationResponse.builder()
                .score(5.0)
                .maxScore(10.0)
                .strengths(List.of("Answer received"))
                .weaknesses(List.of("Evaluation service unavailable"))
                .suggestions(List.of("Try again"))
                .build();
    }

    // ─── Diagram Generation ─────────────────────────────────

    @SuppressWarnings("unchecked")
    public String generateDiagram(String systemDescription) {
        String url = appConfig.getLlmServiceUrl() + "/tools/diagram";
        Map<String, Object> body = Map.of("system_description", systemDescription);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, body, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("diagram");
            }
        } catch (Exception e) {
            log.error("Diagram generation failed: {}", e.getMessage());
        }
        return "graph LR\n    Client --> API_Gateway --> Service --> Database";
    }

    // ─── Hint Generation ────────────────────────────────────

    @SuppressWarnings("unchecked")
    public String generateHint(Map<String, Object> params) {
        String url = appConfig.getLlmServiceUrl() + "/tools/hint";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, params, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("hint");
            }
        } catch (Exception e) {
            log.error("Hint generation failed: {}", e.getMessage());
        }
        return "Think about what happens when the system needs to handle 10x more traffic.";
    }
}
