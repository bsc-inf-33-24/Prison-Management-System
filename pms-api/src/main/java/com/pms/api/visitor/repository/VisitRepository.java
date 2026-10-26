package com.pms.api.visitor.repository;

import com.pms.api.visitor.entity.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VisitRepository extends JpaRepository<Visit, Long>, JpaSpecificationExecutor<Visit> {

    Optional<Visit> findByPublicUuid(UUID publicUuid);

    Page<Visit> findByInmate_IdOrderByStartDateTimeDesc(Long inmateId, Pageable pageable);
}
