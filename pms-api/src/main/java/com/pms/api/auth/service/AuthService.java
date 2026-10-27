package com.pms.api.auth.service;

import com.pms.api.auth.dto.*;
import com.pms.api.auth.entity.PasswordResetToken;
import com.pms.api.auth.repository.PasswordResetTokenRepository;
import com.pms.api.security.jwt.JwtService;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import com.pms.api.user.service.UserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RECOVERY_MESSAGE = "If the account can receive recovery instructions, they have been sent.";
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokens;
    private final RecoveryTokenDelivery recoveryDelivery;
    private final Duration resetTtl;
    private final int maximumLoginAttempts;
    private final Duration lockoutDuration;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository,
                       UserService userService, PasswordEncoder passwordEncoder, PasswordResetTokenRepository resetTokens,
                       RecoveryTokenDelivery recoveryDelivery, @Value("${auth.password-reset-ttl:PT15M}") Duration resetTtl,
                       @Value("${auth.login.max-failed-attempts:5}") int maximumLoginAttempts,
                       @Value("${auth.login.lockout-duration:PT15M}") Duration lockoutDuration) {
        this.authenticationManager = authenticationManager; this.jwtService = jwtService; this.userRepository = userRepository;
        this.userService = userService; this.passwordEncoder = passwordEncoder; this.resetTokens = resetTokens;
        this.recoveryDelivery = recoveryDelivery; this.resetTtl = resetTtl; this.maximumLoginAttempts = maximumLoginAttempts; this.lockoutDuration = lockoutDuration;
    }

    @Transactional(noRollbackFor = org.springframework.security.core.AuthenticationException.class)
    public LoginResult login(AuthRequest request) {
        String login = request.email() == null ? request.username().trim() : request.email();
        User knownUser = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .orElse(null);
        if (knownUser != null && knownUser.isLoginLocked()) throw new AccountLockedException();
        try {
            String canonicalUsername = knownUser == null ? login : knownUser.getUsername();
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(canonicalUsername, request.password()));
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow(() -> new BadCredentialsException("Invalid username or password."));
            user.resetLoginFailures();
            return new LoginResult(new AuthResponse(jwtService.generateToken(user), "Bearer"));
        } catch (org.springframework.security.core.AuthenticationException exception) {
            if (knownUser != null && knownUser.isActive()) knownUser.recordFailedLogin(maximumLoginAttempts, Instant.now().plus(lockoutDuration));
            throw new BadCredentialsException("Invalid username or password.");
        }
    }

    @Transactional
    public PasswordRecoveryResponse forgotPassword(ForgotPasswordRequest request) {
        String rawToken = randomToken();
        String identity = request.username().trim();
        User user = userRepository.findByUsername(identity)
                .or(() -> userRepository.findByEmailIgnoreCase(identity))
                .orElse(null);
        Optional<String> recipient = Optional.empty();
        if (user != null && user.isActive()) {
            resetTokens.deleteOutstandingForUser(user.getId());
            resetTokens.save(new PasswordResetToken(user, hash(rawToken), Instant.now().plus(resetTtl)));
            recipient = Optional.of(user.getEmail() == null ? user.getUsername() : user.getEmail());
        }
        recoveryDelivery.deliver(recipient, rawToken);
        return new PasswordRecoveryResponse(RECOVERY_MESSAGE);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokens.findByTokenHash(hash(request.token())).orElseThrow(InvalidResetTokenException::new);
        if (!token.isUsable(Instant.now()) || !token.getUser().isActive()) throw new InvalidResetTokenException();
        User user = token.getUser();
        token.consume(Instant.now());
        userService.changePassword(user, request.newPassword());
        resetTokens.deleteOutstandingForUser(user.getId());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username).orElseThrow(InvalidCurrentPasswordException::new);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) throw new InvalidCurrentPasswordException();
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) throw new PasswordReuseException();
        userService.changePassword(user, request.newPassword());
        resetTokens.deleteOutstandingForUser(user.getId());
    }

    @Transactional
    public void logout(String username) { userRepository.findByUsername(username).ifPresent(User::invalidateAuthentication); }

    private static String randomToken() { byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public static String hash(String value) { try { return toHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
    private static String toHex(byte[] bytes) { StringBuilder out = new StringBuilder(64); for (byte b : bytes) out.append(String.format("%02x", b)); return out.toString(); }

    public record LoginResult(AuthResponse response) { }
    public static class AccountLockedException extends RuntimeException { }
    public static class InvalidCurrentPasswordException extends RuntimeException { }
    public static class PasswordReuseException extends RuntimeException { }
    public static class InvalidResetTokenException extends RuntimeException { }
}
