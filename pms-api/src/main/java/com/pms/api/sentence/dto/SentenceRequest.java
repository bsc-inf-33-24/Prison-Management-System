package com.pms.api.sentence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SentenceRequest(
        @NotNull(message = "caseId is required")
        @Min(value = 1, message = "caseId must be greater than 0")
        Long caseId,

        @NotNull(message = "inmateId is required")
        @Min(value = 1, message = "inmateId must be greater than 0")
        Long inmateId,

        @NotNull(message = "duration is required")
        @Min(value = 1, message = "duration must be greater than 0")
        Integer duration,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        LocalDate releaseDate,

        @NotNull(message = "status is required")
        @Pattern(regexp = "active|completed|commuted", message = "Status must be one of: active, completed, commuted")
        String status
) {
}
