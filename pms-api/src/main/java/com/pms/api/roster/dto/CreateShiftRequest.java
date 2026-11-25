package com.pms.api.roster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateShiftRequest(
        @NotNull UUID officerId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank @Size(max = 255) String location
) {
}
