package com.pms.api.rehabilitation.dto.program;

import jakarta.validation.constraints.NotBlank;

public record UpdateProgramStatusRequest(
        @NotBlank String status,
        String reason
) {
}
