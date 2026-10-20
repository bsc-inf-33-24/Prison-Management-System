package com.pms.api.auth.controller;

import com.pms.api.auth.dto.AuthRequest;
import com.pms.api.auth.dto.EmailLoginRequest;
import com.pms.api.auth.dto.AuthResponse;
import com.pms.api.auth.dto.UsernameLoginRequest;
import com.pms.api.auth.service.AuthService;
import com.pms.api.auth.dto.ChangePasswordRequest;
import com.pms.api.auth.dto.ForgotPasswordRequest;
import com.pms.api.auth.dto.ResetPasswordRequest;
import com.pms.api.auth.dto.PasswordRecoveryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates a user and returns a signed JWT access token. Supply either username or email.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Choose username login or email login, then enter the password.",
                    content = @Content(schema = @Schema(oneOf = {UsernameLoginRequest.class, EmailLoginRequest.class}),
                            examples = {
                                    @ExampleObject(name = "Log in with username", value = """
                                            {
                                              "username": "records01",
                                              "password": "Records123!"
                                            }
                                            """),
                                    @ExampleObject(name = "Log in with email", value = """
                                            {
                                              "email": "records.officer@example.test",
                                              "password": "Records123!"
                                            }
                                            """)
                            })))
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Log out", description = "Invalidates all currently issued access tokens for the authenticated user.")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails currentUser) {
        authService.logout(currentUser.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Initiate password recovery", description = "Accepts a username or email and returns the same response for registered and unregistered identities. A configured recovery delivery integration is required.")
    public ResponseEntity<PasswordRecoveryResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.accepted().body(authService.forgotPassword(request));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Reset password", description = "Consumes a single-use, short-lived recovery token.")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/change")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password", description = "Changes the authenticated user's password and invalidates existing access tokens.")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails currentUser,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

}
