package com.pms.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Locale;

public record AuthRequest(
    @Schema(description = "Account username or email address", requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 254, example = "records01")
    @Size(max = 254, message = "Username or email must not exceed 254 characters.")
    String identifier,

    @Schema(hidden = true)
    @Size(max = 254, message = "Username or email must not exceed 254 characters.")
    String username,

    @Schema(hidden = true)
    @Email(message = "Email must be a valid email address.")
    @Size(min = 1, max = 254, message = "Email must not exceed 254 characters and must not be blank.")
    String email,

    @Schema(description = "Account password", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 16,
            example = "Records123!")
    @NotBlank(message = "Password is required.")
    @Size(max = 16, message = "Password must not exceed 16 characters.")
    String password
) {
    public AuthRequest {
        if (identifier != null) identifier = identifier.trim();
        if (email != null) email = email.trim().toLowerCase(Locale.ROOT);
    }

    @JsonIgnore
    @AssertTrue(message = "Supply a username or email address in identifier")
    public boolean isExactlyOneLoginIdentityProvided() {
        int suppliedIdentities = (identifier == null || identifier.isBlank() ? 0 : 1)
                + (username == null || username.isBlank() ? 0 : 1)
                + (email == null || email.isBlank() ? 0 : 1);
        return suppliedIdentities == 1;
    }
}
