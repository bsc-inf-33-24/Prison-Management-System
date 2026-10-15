package com.pms.api.cases.repository;

import com.pms.api.cases.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {
    Optional<Case> findByPublicUuid(UUID publicUuid);
}
