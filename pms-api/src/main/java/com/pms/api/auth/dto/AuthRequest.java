package com.pms.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Locale;

public record AuthRequest(
    @Schema(description = "Optional account username. Supply this or email.", maxLength = 254,
            example = "records01")
    @Size(max = 254, message = "Username or email must not exceed 254 characters.")
    String username,

    @Schema(description = "Optional account email. Supply this or username; omit it for username-based login.",
            nullable = true, maxLength = 254, example = "records.officer@example.test")
    @Email(message = "Email must be a valid email address.")
    @Size(min = 1, max = 254, message = "Email must not exceed 254 characters and must not be blank.")
    String email,

    @NotBlank(message = "Password is required.")
    @Size(max = 16, message = "Password must not exceed 16 characters.")
    String password
) {
    public AuthRequest {
        if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
    }

    @JsonIgnore
    @AssertTrue(message = "Supply either username or email")
    public boolean isExactlyOneLoginIdentityProvided() {
        return (username == null || username.isBlank()) != (email == null || email.isBlank());
    }
}
