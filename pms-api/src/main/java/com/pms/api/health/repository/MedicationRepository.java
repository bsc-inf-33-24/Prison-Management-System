package com.pms.api.health.repository;

import com.pms.api.health.entity.Medication;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicationRepository
        extends JpaRepository<Medication, Long>,
                JpaSpecificationExecutor<Medication> {

    Optional<Medication> findByPublicUuid(UUID publicUuid);
}