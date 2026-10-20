package com.pms.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UsernameLoginRequest", description = "Login using the account username.")
public record UsernameLoginRequest(
        @Schema(description = "Account username", requiredMode = Schema.RequiredMode.REQUIRED, example = "records01")
        String username,

        @Schema(description = "Account password", requiredMode = Schema.RequiredMode.REQUIRED, example = "Records123!")
        String password
) {
}
