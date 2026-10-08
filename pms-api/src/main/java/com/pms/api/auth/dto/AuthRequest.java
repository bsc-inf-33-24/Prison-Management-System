package com.pms.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @NotBlank(message = "Username is required.")
    @Size(max = 16, message = "Username must not exceed 16 characters.")
    String username,

    @NotBlank(message = "Password is required.")
    String password
) {
}
