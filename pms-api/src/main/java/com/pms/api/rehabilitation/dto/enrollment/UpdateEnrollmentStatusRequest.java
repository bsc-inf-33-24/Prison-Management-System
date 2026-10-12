package com.pms.api.rehabilitation.dto.enrollment;

import jakarta.validation.constraints.NotBlank;

public record UpdateEnrollmentStatusRequest(
        @NotBlank String status,
        String reason
) {
}
