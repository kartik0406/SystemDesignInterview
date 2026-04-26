package com.sdi.gateway.controller;

import com.sdi.gateway.model.dto.*;
import com.sdi.gateway.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interview")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * Start a new interview session.
     * POST /api/v1/interview/start
     */
    @PostMapping("/start")
    public ResponseEntity<InterviewResponse> startInterview(
            @Valid @RequestBody StartInterviewRequest request) {
        log.info("POST /interview/start — topic={}, company={}", request.getTopic(), request.getCompanyMode());
        InterviewResponse response = interviewService.startInterview(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit an answer for the current round.
     * POST /api/v1/interview/answer
     */
    @PostMapping("/answer")
    public ResponseEntity<InterviewResponse> submitAnswer(
            @Valid @RequestBody SubmitAnswerRequest request) {
        log.info("POST /interview/answer — session={}", request.getSessionId());
        InterviewResponse response = interviewService.submitAnswer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current session state.
     * GET /api/v1/interview/session/{id}
     */
    @GetMapping("/session/{id}")
    public ResponseEntity<InterviewResponse> getSession(@PathVariable UUID id) {
        log.info("GET /interview/session/{}", id);
        InterviewResponse response = interviewService.getSession(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get final evaluation report.
     * GET /api/v1/interview/result/{id}
     */
    @GetMapping("/result/{id}")
    public ResponseEntity<FinalReportResponse> getResult(@PathVariable UUID id) {
        log.info("GET /interview/result/{}", id);
        FinalReportResponse report = interviewService.generateReport(id);
        return ResponseEntity.ok(report);
    }

    /**
     * Request a hint for the current question.
     * POST /api/v1/interview/hint
     */
    @PostMapping("/hint")
    public ResponseEntity<Map<String, String>> requestHint(
            @Valid @RequestBody HintRequest request) {
        log.info("POST /interview/hint — session={}, level={}", request.getSessionId(), request.getHintLevel());
        String hint = interviewService.requestHint(request);
        return ResponseEntity.ok(Map.of("hint", hint));
    }

    /**
     * Get available interview topics.
     * GET /api/v1/interview/topics
     */
    @GetMapping("/topics")
    public ResponseEntity<List<Map<String, String>>> getTopics() {
        log.info("GET /interview/topics");
        return ResponseEntity.ok(interviewService.getTopics());
    }
}
