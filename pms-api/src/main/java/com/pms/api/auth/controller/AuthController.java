package com.pms.api.auth.controller;

import com.pms.api.auth.dto.AuthRequest;
import com.pms.api.auth.dto.AuthResponse;
import com.pms.api.auth.dto.ChangePasswordRequest;
import com.pms.api.auth.dto.ForgotPasswordRequest;
import com.pms.api.auth.dto.PasswordRecoveryResponse;
import com.pms.api.auth.dto.ResetPasswordRequest;
import com.pms.api.auth.service.AuthService;
import com.pms.api.common.error.ApiError;
import com.pms.api.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "01. Authentication")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates a user and returns a signed JWT access token. Use usernameOrEmail for either the account username or its registered email address.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Use the usernameOrEmail field for either the account username or its registered email address.",
                    content = @Content(schema = @Schema(implementation = AuthRequest.class))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication succeeded and a JWT was issued.",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "The login request is invalid.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "The username/email or password is invalid.",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "The account is temporarily locked after too many failed attempts.",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, jakarta.servlet.http.HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        Duration refreshCookieLifetime = Duration.between(Instant.now(), result.response().refreshTokenExpiresAt());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.response().refreshToken(), refreshCookieLifetime).toString());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Validates a refresh token from the secure cookie and issues a new access token while rotating the refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A new access token was issued.",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "The refresh token is missing, expired, revoked, or invalid.",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<AuthResponse> refresh(@CookieValue(value = "refresh_token", required = false) String refreshTokenCookie,
                                              @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader,
                                              jakarta.servlet.http.HttpServletResponse response) {
        String refreshToken = refreshTokenCookie != null && !refreshTokenCookie.isBlank() ? refreshTokenCookie : refreshTokenHeader;
        AuthService.RefreshResult result = authService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken(), Duration.ofDays(7)).toString());
        return ResponseEntity.ok(result.response());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current authenticated user", description = "Returns the authenticated user's profile as known by the server.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated user profile was returned.",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(authService.me(currentUser.getUsername()));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Log out", description = "Revokes all active sessions for the authenticated user and clears the refresh-token cookie.")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails currentUser,
                                      jakarta.servlet.http.HttpServletResponse response) {
        authService.logout(currentUser.getUsername());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Initiate password recovery", description = "Accepts the account's registered email address and emails a six-digit reset code when a configured recovery delivery integration is available. The response is the same for registered and unregistered addresses.")
    public ResponseEntity<PasswordRecoveryResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.accepted().body(authService.forgotPassword(request));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Reset password", description = "Consumes a single-use, short-lived six-digit recovery code.")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/change")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password", description = "Changes the authenticated user's password and invalidates all active sessions.")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails currentUser,
                                                @Valid @RequestBody ChangePasswordRequest request,
                                                jakarta.servlet.http.HttpServletResponse response) {
        authService.changePassword(currentUser.getUsername(), request);
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie refreshTokenCookie(String token, Duration maxAge) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

}
