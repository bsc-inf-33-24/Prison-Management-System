package com.pms.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "AuthRequest", description = "Authenticate with an account username or its email address and password.")
public class AuthRequest {
    private final String usernameOrEmail;
    private final String password;

    @JsonCreator
    public AuthRequest(
            @JsonProperty("usernameOrEmail") String usernameOrEmail,
            @JsonProperty("password") String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }

    @Schema(description = "Account username or registered email address used to sign in", requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 254, example = "records01")
    @NotBlank(message = "Username or email is required.")
    @Size(max = 254, message = "Username or email must not exceed 254 characters.")
    @JsonProperty("usernameOrEmail")
    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    @Schema(description = "Account password", requiredMode = Schema.RequiredMode.REQUIRED, example = "Records123!")
    @NotBlank(message = "Password is required.")
    @Size(max = 16, message = "Password must not exceed 16 characters.")
    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) {
        throw new IllegalArgumentException("Unexpected login field: " + name);
    }
}
