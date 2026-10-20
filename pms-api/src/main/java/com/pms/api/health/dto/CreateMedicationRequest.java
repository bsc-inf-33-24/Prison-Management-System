package com.pms.api.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateMedicationRequest(
        @NotNull UUID consultationId,
        @NotBlank @Size(max = 255) String drugName,
        @Size(max = 255) String dosage,
        @Size(max = 255) String frequency,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Size(max = 50) String status
) {
}