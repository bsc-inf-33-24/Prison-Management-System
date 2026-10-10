package com.pms.api.cases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrimeRequest(
        @NotBlank(message = "offenceName is required") @Size(max = 150) String offenceName,
        @Size(max = 500) String description,
        @NotBlank(message = "category is required") @Size(max = 100) String category
) {
}
