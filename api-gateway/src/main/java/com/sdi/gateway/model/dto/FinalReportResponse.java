package com.sdi.gateway.model.dto;

import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalReportResponse {

    private UUID sessionId;
    private String topic;
    private String companyMode;
    private double overallScore;
    private int totalRounds;
    private List<RoundSummary> rounds;
    private List<String> overallStrengths;
    private List<String> overallWeaknesses;
    private List<String> improvementSuggestions;
    private Map<String, Double> aggregatedRubric;
    private String architectureDiagram;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoundSummary {
        private int roundNumber;
        private String question;
        private String answer;
        private double score;
        private String difficulty;
        private List<String> strengths;
        private List<String> weaknesses;
    }
}
