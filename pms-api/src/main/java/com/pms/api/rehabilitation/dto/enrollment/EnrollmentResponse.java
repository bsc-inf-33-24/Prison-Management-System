package com.pms.api.rehabilitation.dto.enrollment;

import com.pms.api.rehabilitation.entity.Enrollment;

import java.time.LocalDate;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID inmateId,
        String inmateName,
        UUID programId,
        String programName,
        LocalDate enrollmentDate,
        String status
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getPublicUuid(),
                enrollment.getInmate().getPublicUuid(),
                enrollment.getInmate().getFirstName() + " " + enrollment.getInmate().getLastName(),
                enrollment.getProgram().getPublicUuid(),
                enrollment.getProgram().getName(),
                enrollment.getEnrollmentDate(),
                enrollment.getStatus()
        );
    }
}
