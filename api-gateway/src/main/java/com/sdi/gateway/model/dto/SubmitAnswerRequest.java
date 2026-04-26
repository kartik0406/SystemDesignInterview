package com.sdi.gateway.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAnswerRequest {

    @NotNull(message = "Session ID is required")
    private UUID sessionId;

    @NotBlank(message = "Answer cannot be empty")
    private String answer;
}
