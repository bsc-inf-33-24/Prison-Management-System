package com.pms.api.rehabilitation.dto.enrollment;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEnrollmentRequest(
        @NotNull(message = "the inmateId is required")
	UUID inmateId,

        @NotNull(message = "the programId is required")
	UUID programId,

        @NotNull(message = "the enrollment date is missing")
	LocalDate enrollmentDate
) {
}
