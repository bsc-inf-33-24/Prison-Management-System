package com.pms.api.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAllergyRequest(
        @NotNull UUID inmateId,
        @NotBlank @Size(max = 255) String allergen,
        String reaction,
        @Size(max = 50) String severity,
        @NotNull LocalDate recordedDate,
        String status
) {}