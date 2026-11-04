package com.pms.api.rehabilitation.dto.program;

import com.pms.api.rehabilitation.entity.RehabilitationProgram;
import java.util.UUID;

public record ProgramDetailResponse(
        UUID id,
        String name,
        String description,
        String instructor,
        UUID instructorUserId,
        String instructorDisplayName,
        String schedule,
        String status,
        long activeEnrollmentCount,
        long completedEnrollmentCount
) {
    public static ProgramDetailResponse from(RehabilitationProgram program,
                                              long activeEnrollmentCount,
                                              long completedEnrollmentCount) {
        return new ProgramDetailResponse(
                program.getPublicUuid(),
                program.getName(),
                program.getDescription(),
                program.getInstructor(),
                program.getInstructorUser() == null ? null : program.getInstructorUser().getPublicUuid(),
                program.getInstructorUser() == null ? null : program.getInstructorUser().getFullName(),
                program.getSchedule(),
                program.getStatus(),
                activeEnrollmentCount,
                completedEnrollmentCount
        );
    }
}
