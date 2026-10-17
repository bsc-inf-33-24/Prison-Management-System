package com.pms.api.rehabilitation.dto.enrollment;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEnrollmentRequest(
        @NotNull UUID inmateId,
        @NotNull UUID programId,
        @NotNull LocalDate enrollmentDate
) {
}
