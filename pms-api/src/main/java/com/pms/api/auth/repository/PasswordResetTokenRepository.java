package com.pms.api.auth.repository;

import com.pms.api.auth.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Modifying
    @Query("delete from PasswordResetToken t where t.user.id = :userId and t.consumedAt is null")
    void deleteOutstandingForUser(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
