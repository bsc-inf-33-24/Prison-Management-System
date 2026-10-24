package com.pms.api.health.repository;

import com.pms.api.health.entity.MedicalCondition;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicalConditionRepository
        extends JpaRepository<MedicalCondition, Long>,
                JpaSpecificationExecutor<MedicalCondition> {

    Optional<MedicalCondition> findByPublicUuid(UUID publicUuid);
}
