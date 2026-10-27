package com.pms.api.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateUserWithoutEmailRequest", description = "Create a user without an email address.")
public record CreateUserWithoutEmailRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Records Officer")
        String fullName,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "records01", maxLength = 16)
        String username,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Records123!")
        String password,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Records Officer")
        String role
) {
}
