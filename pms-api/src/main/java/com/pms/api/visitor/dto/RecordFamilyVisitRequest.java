package com.pms.api.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RecordFamilyVisitRequest(
        @NotBlank @Size(max = 255) String visitorFullName,
        @NotNull UUID inmateId,
        @NotNull LocalDate visitDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Size(max = 1000) String itemsBrought
) { }
