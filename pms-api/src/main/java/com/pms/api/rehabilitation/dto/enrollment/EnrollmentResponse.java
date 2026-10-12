package com.pms.api.rehabilitation.dto.enrollment;

import com.pms.api.rehabilitation.entity.Enrollment;

import java.time.LocalDate;

public record EnrollmentResponse(
        Long id,
        Long inmateId,
        Long programId,
        LocalDate enrollmentDate,
        String status
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getInmate().getId(),
                enrollment.getProgram().getId(),
                enrollment.getEnrollmentDate(),
                enrollment.getStatus()
        );
    }
}
