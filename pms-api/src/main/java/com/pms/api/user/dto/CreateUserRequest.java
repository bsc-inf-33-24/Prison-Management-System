package com.pms.api.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Locale;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateUserRequest(
    @Schema(description = "User's full name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Records Officer")
        @NotBlank(message = "Full name is required.")
        String fullName,

        @Schema(description = "Unique account username", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 16, example = "records01")
        @NotBlank(message = "Username is required.")
        @Size(max = 16, message = "Username must not exceed 16 characters.")
        String username,

        @Schema(description = "Required email address. The system sends a generated temporary password to this address; surrounding spaces are trimmed and the address is lowercased.",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 254, example = "records.officer@example.test")
        @NotBlank(message = "Email is required for account setup.")
        @Email(message = "Email must be a valid email address.")
        @Size(min = 1, max = 254, message = "Email must not exceed 254 characters and must not be blank.")
        String email,

        @Schema(description = "User role", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"Super Admin", "Commissioner", "Transfer Officer", "Records Officer", "Disciplinary Officer", "Case Officer", "Visitor Officer", "Instructor", "Medical Officer", "Duty Officer", "Program Facilitator"},
                example = "Records Officer")
        @NotBlank(message = "Role is required.")
        String role,

        @Schema(description = "Prison this staff account belongs to. Required for prison-scoped roles; Super Admin, Commissioner, and Transfer Officer accounts are system-wide.", nullable = true)
        UUID prisonId
) {
    public CreateUserRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
