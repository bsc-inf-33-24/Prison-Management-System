package com.pms.api.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                   UserRepository userRepository) {
        this.jwtService = jwtService; this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.claims(header.substring(7));
                User account = userRepository.findByUsername(claims.getSubject())
                        .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password."));
                Number versionClaim = claims.get("ver", Number.class);
                String userUuidClaim = claims.get("uid", String.class);
                long version = versionClaim == null ? -1 : versionClaim.longValue();
                UUID userUuid = userUuidClaim == null ? null : UUID.fromString(userUuidClaim);
                UserDetails user = userDetailsService.loadUserByUsername(account.getUsername());
                if (userUuid != null && userUuid.equals(account.getPublicUuid()) && version == account.getAuthenticationVersion()
                        && user.isEnabled() && user.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | UsernameNotFoundException | IllegalArgumentException exception) {
                LOGGER.warn("Rejected invalid bearer token ({})", exception.getClass().getSimpleName());
            }
        }
        filterChain.doFilter(request, response);
    }
}
