package com.pms.api.rehabilitation.dto.program;

import com.pms.api.rehabilitation.entity.RehabilitationProgram;

public record ProgramResponse(
        Long id,
        String name,
        String description,
        String instructor,
        String schedule,
        String status
) {
    public static ProgramResponse from(RehabilitationProgram program) {
        return new ProgramResponse(
                program.getId(),
                program.getName(),
                program.getDescription(),
                program.getInstructor(),
                program.getSchedule(),
                program.getStatus()
        );
    }
}
