package com.pms.api.sentence.repository;

import com.pms.api.sentence.entity.InmateOffence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InmateOffenceRepository extends JpaRepository<InmateOffence, Long> {
    List<InmateOffence> findByInmate_IdOrderByCreatedAtDescIdDesc(Long inmateId);
    Optional<InmateOffence> findByPublicUuid(UUID publicUuid);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from InmateOffence o where o.publicUuid = :uuid")
    Optional<InmateOffence> findByPublicUuidForUpdate(@Param("uuid") UUID publicUuid);
}
