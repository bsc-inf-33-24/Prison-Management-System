package com.pms.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthRequest(
    @NotBlank(message = "Username or email is required.")
    @Size(max = 254, message = "Username or email must not exceed 254 characters.")
    @Schema(description = "Username or email address; the field name is retained for API compatibility.", maxLength = 254)
    String username,

    @NotBlank(message = "Password is required.")
    @Size(max = 16, message = "Password must not exceed 16 characters.")
    String password
) {
}
