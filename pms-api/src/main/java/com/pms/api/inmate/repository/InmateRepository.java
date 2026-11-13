package com.pms.api.inmate.repository;

import com.pms.api.inmate.entity.Inmate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InmateRepository extends JpaRepository<Inmate, Long>, JpaSpecificationExecutor<Inmate> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "UPDATE inmates SET current_prison_id = :prisonId WHERE current_prison_id IS NULL", nativeQuery = true)
    int assignPrisonToUnassigned(@org.springframework.data.repository.query.Param("prisonId") Long prisonId);

    Optional<Inmate> findByPublicUuid(UUID publicUuid);

}
