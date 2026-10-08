package com.pms.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Full name is required.")
        String fullName,

        @NotBlank(message = "Username is required.")
        @Size(max = 16, message = "Username must not exceed 16 characters.")
        String username,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        String password,

        @NotBlank(message = "Role is required.")
        String role
) {
}
