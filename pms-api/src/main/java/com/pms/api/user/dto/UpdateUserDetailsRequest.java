package com.pms.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.Set;

/** Fields are optional; null values are left unchanged. */
public record UpdateUserDetailsRequest(
        @Size(min = 1, max = 255, message = "Full name must not be blank and must not exceed 255 characters.")
        String fullName,

        @Size(min = 1, max = 16, message = "Username must not be blank and must not exceed 16 characters.")
        String username,

        @Schema(description = "Optional contact email. Omit or set to null to leave unchanged.", nullable = true,
                maxLength = 254)
        @Email(message = "Email must be a valid email address.")
        @Size(min = 1, max = 254, message = "Email must not be blank and must not exceed 254 characters.")
        String email,

        String role,

        Set<String> permissions
) {
    public UpdateUserDetailsRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
