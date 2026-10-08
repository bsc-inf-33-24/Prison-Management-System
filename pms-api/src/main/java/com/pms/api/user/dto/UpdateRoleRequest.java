package com.pms.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public record UpdateRoleRequest(
        @NotBlank(message = "Role is required.")
        String role,
        Set<String> permissions
) {
}
