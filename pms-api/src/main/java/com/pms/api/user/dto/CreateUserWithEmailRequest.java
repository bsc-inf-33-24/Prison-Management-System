package com.pms.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateUserWithEmailRequest", description = "Create a user with a valid email address.")
public record CreateUserWithEmailRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Records Officer")
        String fullName,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "records01", maxLength = 16)
        String username,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "records.officer@example.test", format = "email")
        String email,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Records123!")
        String password,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Records Officer")
        String role
) {
}
