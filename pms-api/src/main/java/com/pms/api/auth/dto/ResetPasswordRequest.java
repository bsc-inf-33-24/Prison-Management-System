package com.pms.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank @Pattern(regexp = "[0-9]{6}", message = "Recovery code must be exactly 6 digits.")
        String code,
        @NotBlank @Size(min = 8, max = 16) String newPassword
        
) { }
