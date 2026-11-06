package com.pms.api.prison.repository;

import com.pms.api.prison.entity.Prison;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PrisonRepository extends JpaRepository<Prison, Long>, JpaSpecificationExecutor<Prison> {
    Optional<Prison> findByPublicUuid(UUID id);
    boolean existsByCodeIgnoreCase(String code);
}
