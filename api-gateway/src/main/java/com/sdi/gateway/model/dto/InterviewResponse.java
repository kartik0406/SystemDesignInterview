package com.sdi.gateway.model.dto;

import com.sdi.gateway.model.enums.CompanyMode;
import com.sdi.gateway.model.enums.DifficultyLevel;
import com.sdi.gateway.model.enums.SessionStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponse {

    private UUID sessionId;
    private String topic;
    private CompanyMode companyMode;
    private int currentRound;
    private int maxRounds;
    private DifficultyLevel difficulty;
    private SessionStatus status;
    private String question;
    private String topicArea;

    // Present only after answer submission
    private EvaluationResponse evaluation;
    private boolean isLastRound;
}
