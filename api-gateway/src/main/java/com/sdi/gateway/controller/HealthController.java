package com.sdi.gateway.controller;

import com.sdi.gateway.agent.AgentCard;
import com.sdi.gateway.agent.InterviewAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final InterviewAgent interviewAgent;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "SDI API Gateway",
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * A2A Agent discovery endpoint.
     * Returns all registered agent cards.
     */
    @GetMapping("/.well-known/agent-cards")
    public ResponseEntity<List<AgentCard>> agentCards() {
        return ResponseEntity.ok(interviewAgent.discoverAgents());
    }
}
