package com.pms.api.rehabilitation.dto.program;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProgramRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 10000) String description,
        @NotBlank @Size(max = 255) String instructor,
        @Size(max = 255) String schedule
) {
}
