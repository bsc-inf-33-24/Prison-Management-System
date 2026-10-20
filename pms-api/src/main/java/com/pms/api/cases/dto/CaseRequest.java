package com.pms.api.cases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CaseRequest(
        @NotNull(message = "inmateId is required") UUID inmateId,
        @NotBlank(message = "caseNumber is required") @Size(max = 80) String caseNumber,
        @NotBlank(message = "court is required") @Size(max = 150) String court,
        @NotBlank(message = "crimes is required") String crimes,
        @NotNull(message = "caseStartDate is required") LocalDate caseStartDate,
        @NotNull(message = "caseEndDate is required") LocalDate caseEndDate
) {
}
