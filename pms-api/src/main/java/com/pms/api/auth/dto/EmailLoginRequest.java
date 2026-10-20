package com.pms.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EmailLoginRequest", description = "Login using the account email address.")
public record EmailLoginRequest(
        @Schema(description = "Email address associated with the account", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "records.officer@example.test", format = "email")
        String email,

        @Schema(description = "Account password", requiredMode = Schema.RequiredMode.REQUIRED, example = "Records123!")
        String password
) {
}
