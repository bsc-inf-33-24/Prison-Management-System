package com.pms.api.auth.repository;

import com.pms.api.auth.entity.RefreshToken;
import com.pms.api.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("select rt from RefreshToken rt where rt.user = :user and rt.revokedAt is null and rt.expiresAt > :now")
    List<RefreshToken> findActiveByUser(@Param("user") User user, @Param("now") Instant now);

    @Modifying
    @Query("update RefreshToken rt set rt.revokedAt = :now where rt.user = :user and rt.revokedAt is null")
    void revokeAllActiveForUser(@Param("user") User user, @Param("now") Instant now);
}
