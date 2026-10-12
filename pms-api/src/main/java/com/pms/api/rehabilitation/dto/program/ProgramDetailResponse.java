package com.pms.api.rehabilitation.dto.program;

import com.pms.api.rehabilitation.entity.RehabilitationProgram;

public record ProgramDetailResponse(
        Long id,
        String name,
        String description,
        String instructor,
        String schedule,
        String status,
        long activeEnrollmentCount,
        long completedEnrollmentCount
) {
    public static ProgramDetailResponse from(RehabilitationProgram program,
                                              long activeEnrollmentCount,
                                              long completedEnrollmentCount) {
        return new ProgramDetailResponse(
                program.getId(),
                program.getName(),
                program.getDescription(),
                program.getInstructor(),
                program.getSchedule(),
                program.getStatus(),
                activeEnrollmentCount,
                completedEnrollmentCount
        );
    }
}
