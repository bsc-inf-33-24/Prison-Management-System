package com.pms.api.auth.controller;

import com.pms.api.auth.dto.AuthRequest;
import com.pms.api.auth.dto.AuthResponse;
import com.pms.api.auth.service.AuthService;
import com.pms.api.auth.dto.ChangePasswordRequest;
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
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails currentUser,
                                                @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

}
