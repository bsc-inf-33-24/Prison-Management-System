package com.pms.api.sentence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SentenceRequest(
        Long caseId,

        @NotNull
        Long inmateId,

        @NotNull
        @Min(1)
        Integer duration,

        @NotNull
        LocalDate startDate,

        LocalDate releaseDate,

        @NotNull
        @Pattern(regexp = "active|completed|commuted", message = "Status must be one of: active, completed, commuted")
        String status
) {
}
