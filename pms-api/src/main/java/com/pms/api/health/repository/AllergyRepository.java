package com.pms.api.health.repository;

import com.pms.api.health.entity.Allergy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AllergyRepository extends
        JpaRepository<Allergy, Long>,
        JpaSpecificationExecutor<Allergy> {

    Optional<Allergy> findByPublicUuid(UUID publicUuid);
}