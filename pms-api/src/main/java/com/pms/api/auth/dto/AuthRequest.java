package com.pms.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AuthRequest", description = "Authenticate with an account username or its email address and password.")
public record AuthRequest(
    @Schema(description = "Account username or associated email address", requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 254, example = "records01")
    @NotBlank(message = "Username or email is required.")
    @Size(max = 254, message = "Username or email must not exceed 254 characters.")
    String username,

    @Schema(description = "Account password", requiredMode = Schema.RequiredMode.REQUIRED, example = "Records123!")
    @NotBlank(message = "Password is required.")
    @Size(max = 16, message = "Password must not exceed 16 characters.")
    String password
) {
}
