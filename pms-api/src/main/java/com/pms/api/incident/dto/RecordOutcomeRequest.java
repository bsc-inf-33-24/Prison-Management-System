package com.pms.api.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecordOutcomeRequest(

        @NotBlank(message = "Outcome is required.")
        @Size(max = 255, message = "Outcome must be at most 255 characters.")
        String outcome
) {
}
