package com.pms.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Locale;

public record CreateUserRequest(
    @Schema(description = "User's full name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Records Officer")
        @NotBlank(message = "Full name is required.")
        String fullName,

        @Schema(description = "Unique account username", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 16, example = "records01")
        @NotBlank(message = "Username is required.")
        @Size(max = 16, message = "Username must not exceed 16 characters.")
        String username,

        @Schema(description = "Optional contact email. Omit or set to null when the user has no email. When supplied, it must be valid; surrounding spaces are trimmed and the address is lowercased.",
                nullable = true, maxLength = 254, example = "records.officer@example.test")
        @Email(message = "Email must be a valid email address.")
        @Size(min = 1, max = 254, message = "Email must not exceed 254 characters and must not be blank.")
        String email,

        @Schema(description = "Password must be 8 to 16 characters and contain at least one digit and one punctuation mark.",
                requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, maxLength = 16, example = "Records123!")
        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters.")
        String password,

        @Schema(description = "User role", requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"Super Admin", "Records Officer", "Disciplinary Officer", "Case Officer", "Visitor Officer", "Rehabilitation Officer", "Medical Officer", "Duty Officer"},
                example = "Records Officer")
        @NotBlank(message = "Role is required.")
        String role
) {
    public CreateUserRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
