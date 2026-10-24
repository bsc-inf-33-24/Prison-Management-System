package com.pms.api.health.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateHealthRecordRequest(
        @NotNull UUID inmateId,
        @NotNull LocalDate screeningDate,
        @Size(max = 255) String screeningResults,
        @Size(max = 10) String bloodGroup,
        String status
) {}
