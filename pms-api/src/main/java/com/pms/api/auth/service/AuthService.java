package com.pms.api.auth.service;

import com.pms.api.auth.dto.AuthRequest;
import com.pms.api.auth.dto.AuthResponse;
import com.pms.api.auth.dto.ChangePasswordRequest;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.user.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final int maximumLoginAttempts;
    private final Duration lockoutDuration;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository,
                       UserService userService,
                       PasswordEncoder passwordEncoder,
                       @org.springframework.beans.factory.annotation.Value("${auth.login.max-failed-attempts:5}") int maximumLoginAttempts,
                       @org.springframework.beans.factory.annotation.Value("${auth.login.lockout-duration:PT15M}") Duration lockoutDuration) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.maximumLoginAttempts = maximumLoginAttempts;
        this.lockoutDuration = lockoutDuration;
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public LoginResult login(AuthRequest request) {
        String username = request.username().trim();
        User knownUser = userRepository.findByUsername(username).orElse(null);
        if (knownUser != null && knownUser.isLoginLocked()) {
            throw new AccountLockedException();
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException exception) {
            if (knownUser != null && knownUser.isActive()) {
                knownUser.recordFailedLogin(maximumLoginAttempts, Instant.now().plus(lockoutDuration));
            }
            throw exception;
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
        user.resetLoginFailures();
        return new LoginResult(new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                new AuthResponse.UserInfo(user.getId(), user.getFullName(), user.getRole())
        ));
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        userService.changePassword(user, request.newPassword());
    }

    public record LoginResult(AuthResponse response) { }
    public static class AccountLockedException extends RuntimeException { }
    public static class InvalidCurrentPasswordException extends RuntimeException { }
}
