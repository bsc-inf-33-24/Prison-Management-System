package com.pms.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "token is required") String token,
        @NotBlank(message = "new password is required") @Size(min = 8, max = 16) String newPassword
) { }
