package com.pms.api.sentence.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;

/** Optional sentence fields; null values are left unchanged. */
public record UpdateSentenceDetailsRequest(
        UUID caseId,
        UUID inmateId,
        @Min(value = 1, message = "Duration must be greater than 0.")
        Integer duration,
        LocalDate startDate,
        LocalDate releaseDate,
        @Pattern(regexp = "active|completed|commuted", message = "Status must be one of: active, completed, commuted.")
        String status
) {
}
