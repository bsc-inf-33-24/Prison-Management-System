package com.pms.api.rehabilitation.dto.program;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProgramRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 10000) String description,
        @Size(max = 255) String schedule,
        UUID instructorUserId
) {
    public CreateProgramRequest(String name, String description, String schedule) {
        this(name, description, schedule, null);
    }
}
