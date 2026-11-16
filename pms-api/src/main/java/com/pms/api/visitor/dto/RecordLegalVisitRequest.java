package com.pms.api.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RecordLegalVisitRequest(
        @NotNull UUID inmateId,
        @NotNull LocalDate visitDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank @Size(max = 255) String legalCaseNumber,
        @Size(max = 255) String legalRepresentative
) { }
