package com.pms.api.inmate.repository;

import com.pms.api.inmate.entity.CustodyStay;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustodyStayRepository extends JpaRepository<CustodyStay, Long> {
    Optional<CustodyStay> findFirstByInmate_IdAndEndedAtIsNullOrderByIdDesc(Long inmateId);

    @Modifying
    @Query("update CustodyStay s set s.endedAt = :endedAt where s.inmate.id = :inmateId and s.endedAt is null")
    int endActiveStay(@Param("inmateId") Long inmateId, @Param("endedAt") Instant endedAt);
}
