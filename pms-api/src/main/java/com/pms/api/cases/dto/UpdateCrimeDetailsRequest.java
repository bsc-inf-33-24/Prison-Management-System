package com.pms.api.cases.dto;

import jakarta.validation.constraints.Size;

/** Optional crime catalog fields; null values are left unchanged. */
public record UpdateCrimeDetailsRequest(
        @Size(min = 1, max = 150, message = "Offence name must not be blank and must not exceed 150 characters.")
        String offenceName,
        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String description,
        @Size(min = 1, max = 100, message = "Category must not be blank and must not exceed 100 characters.")
        String category
) {
}
