package com.pms.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Locale;

public record CreateUserRequest(
        @NotBlank(message = "Full name is required.")
        String fullName,

        @NotBlank(message = "Username is required.")
        @Size(max = 16, message = "Username must not exceed 16 characters.")
        String username,

        @Schema(description = "Optional contact email. Blank values are rejected; accepted values are trimmed and lowercased.",
                nullable = true, maxLength = 254)
        @Email(message = "Email must be a valid email address.")
        @Size(min = 1, max = 254, message = "Email must not exceed 254 characters and must not be blank.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters.")
        String password,

        @NotBlank(message = "Role is required.")
        String role
) {
    public CreateUserRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
