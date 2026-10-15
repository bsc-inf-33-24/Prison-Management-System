package com.pms.api.rehabilitation.dto.program;

import com.pms.api.rehabilitation.entity.RehabilitationProgram;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String name,
        String description,
        String instructor,
        String schedule,
        String status
) {
    public static ProgramResponse from(RehabilitationProgram program) {
        return new ProgramResponse(
                program.getPublicUuid(),
                program.getName(),
                program.getDescription(),
                program.getInstructor(),
                program.getSchedule(),
                program.getStatus()
        );
    }
}
