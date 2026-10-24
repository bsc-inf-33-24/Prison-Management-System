package com.pms.api.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateMedicalConditionRequest(
        @NotNull UUID healthRecordId,
        @NotBlank @Size(max = 255) String conditionType,
        @NotBlank @Size(max = 255) String conditionName,
        @Size(max = 255) String severity,
        @NotNull LocalDate dateRecorded,
        String status
) {}
