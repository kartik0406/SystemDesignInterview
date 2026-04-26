package com.sdi.gateway.model.dto;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HintRequest {

    @NotNull(message = "Session ID is required")
    private UUID sessionId;

    @Builder.Default
    private int hintLevel = 1;   // 1 = nudge, 2 = direction, 3 = partial solution
}
