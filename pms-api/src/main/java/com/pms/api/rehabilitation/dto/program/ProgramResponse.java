package com.pms.api.rehabilitation.dto.program;

import com.pms.api.rehabilitation.entity.RehabilitationProgram;
import java.util.List;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String name,
        String description,
        String instructor,
        String schedule,
        String status,
        List<EnrolledInmateResponse> enrolledInmates
) {
    public static ProgramResponse from(RehabilitationProgram program) {
        return from(program, List.of());
    }

    public static ProgramResponse from(RehabilitationProgram program,
                                       List<EnrolledInmateResponse> enrolledInmates) {
        return new ProgramResponse(
                program.getPublicUuid(),
                program.getName(),
                program.getDescription(),
                program.getInstructor(),
                program.getSchedule(),
                program.getStatus(),
                enrolledInmates
        );
    }
}
