package com.pms.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "Username is required.")
        @Size(max = 16, message = "Username must not exceed 16 characters.")
        String username
) { }
