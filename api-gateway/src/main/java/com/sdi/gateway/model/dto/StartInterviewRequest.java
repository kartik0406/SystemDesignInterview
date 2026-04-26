package com.sdi.gateway.model.dto;

import com.sdi.gateway.model.enums.CompanyMode;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartInterviewRequest {

    @NotBlank(message = "Topic is required")
    private String topic;

    @Builder.Default
    private CompanyMode companyMode = CompanyMode.GENERAL;
}
