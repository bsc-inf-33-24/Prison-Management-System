package com.pms.api.security.jwt;

import com.pms.api.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-token-ttl}") Duration accessTokenTtl,
                      @Value("${jwt.refresh-token-ttl:P7D}") Duration refreshTokenTtl) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getPublicUuid().toString())
                .claim("ver", user.getAuthenticationVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(signingKey)
                .compact();
    }

    public RefreshTokenDetails generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenTtl);
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getPublicUuid().toString())
                .claim("ver", user.getAuthenticationVersion())
                .claim("typ", "refresh")
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new RefreshTokenDetails(token, tokenId, expiresAt);
    }

    public RefreshTokenDetails generateRefreshToken(User user, Instant expiresAt) {
        Instant now = Instant.now();
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getPublicUuid().toString())
                .claim("ver", user.getAuthenticationVersion())
                .claim("typ", "refresh")
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new RefreshTokenDetails(token, tokenId, expiresAt);
    }

    public Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public Claims extractClaims(String token) {
        return claims(token);
    }

    public String username(String token) {
        return claims(token).getSubject();
    }

    public record RefreshTokenDetails(String token, String tokenId, Instant expiresAt) { }
}
