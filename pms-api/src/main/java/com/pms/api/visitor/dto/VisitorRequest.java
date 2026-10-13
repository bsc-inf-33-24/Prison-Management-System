package com.pms.api.visitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VisitorRequest(
        @NotBlank(message = "fullName is required") String fullName,
        @NotBlank(message = "idNumber is required")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "idNumber is invalid") String idNumber,
        @Pattern(regexp = "^[+0-9\\s()-]{7,20}$", message = "phone is invalid") String phone,
        @NotBlank(message = "relationshipToInmate is required") String relationshipToInmate
) {
}
