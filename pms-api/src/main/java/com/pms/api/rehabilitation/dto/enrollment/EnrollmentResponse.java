package com.pms.api.rehabilitation.dto.enrollment;

import com.pms.api.rehabilitation.entity.Enrollment;

import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID inmateId,
        UUID programId,
        LocalDate enrollmentDate,
        String status
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getPublicUuid(),
                enrollment.getInmate().getPublicUuid(),
                enrollment.getProgram().getPublicUuid(),
                enrollment.getEnrollmentDate(),
                enrollment.getStatus()
        );
    }
}
