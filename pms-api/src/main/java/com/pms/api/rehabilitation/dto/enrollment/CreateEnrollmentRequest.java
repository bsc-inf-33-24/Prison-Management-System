package com.pms.api.rehabilitation.dto.enrollment;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateEnrollmentRequest(
        @NotNull Long inmateId,
        @NotNull Long programId,
        @NotNull LocalDate enrollmentDate
) {
}
