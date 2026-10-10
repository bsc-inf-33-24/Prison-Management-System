package com.pms.api.cases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CaseStatusRequest(
        @NotNull(message = "status is required") String status,
        String reason
) {
}
