package com.sdi.gateway.model.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResponse {

    private double score;
    private double maxScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
    private Map<String, Double> rubricBreakdown;
    private String difficultyAdjustment;
}
