package com.pms.api.health.repository;

import com.pms.api.health.entity.HealthRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long>,
        JpaSpecificationExecutor<HealthRecord> {
    Optional<HealthRecord> findByPublicUuid(UUID publicUuid);
}
