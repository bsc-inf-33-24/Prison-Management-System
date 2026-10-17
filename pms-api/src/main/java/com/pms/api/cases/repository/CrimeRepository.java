package com.pms.api.cases.repository;

import com.pms.api.cases.entity.Crime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

public interface CrimeRepository extends JpaRepository<Crime, Long>, JpaSpecificationExecutor<Crime> {
    Optional<Crime> findByPublicUuid(UUID publicUuid);
}
