package com.pms.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record ForgotPasswordRequest(
        @NotBlank(message = "User email is required.")
        @Size(max = 254, message = "User email must not exceed 254 characters.")
        @Schema(description = "Email address registered to the account.", example = "records01@example.com", maxLength = 254)
        String userEmail
) { }
