package com.pms.api.security.config;

import com.pms.api.common.audit.AuditService;
import com.pms.api.common.error.ApiError;
import com.pms.api.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuditService auditService) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/api/v1/auth/login",
                    "/api/v1/auth/password/forgot",
                    "/api/v1/auth/password/reset",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> {
                    recordDeniedAccess(
                            auditService, request,
                            "AUTHENTICATION_REQUIRED",
                            "Missing or invalid authentication.");
                    writeSecurityError(
                            response, HttpStatus.UNAUTHORIZED,
                            "Authentication is required.");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    recordDeniedAccess(
                            auditService, request,
                            "ACCESS_DENIED",
                            "Insufficient role or permission.");
                    writeSecurityError(
                            response, HttpStatus.FORBIDDEN,
                            "You are not authorized to access this resource.");
                })
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void recordDeniedAccess(
            AuditService auditService,
            HttpServletRequest request,
            String action,
            String reason) {
        try {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            String username = null;
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal()
                            instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                username = userDetails.getUsername();
            }

            auditService.recordSecurityEvent(
                    username, action, request.getRequestURI(), reason);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to record security audit event: {}", action);
        }
    }

    private static void writeSecurityError(
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(status, message);
        response.getWriter().write(
                "{\"status\":" + error.status()
                + ",\"error\":\"" + error.error()
                + "\",\"message\":\"" + error.message() + "\"}");
    }
}
