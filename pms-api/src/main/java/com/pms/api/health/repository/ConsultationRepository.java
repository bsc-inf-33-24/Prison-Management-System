package com.pms.api.health.repository;

import com.pms.api.health.entity.Consultation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConsultationRepository
        extends JpaRepository<Consultation, Long>,
                JpaSpecificationExecutor<Consultation> {

    Optional<Consultation> findByPublicUuid(UUID publicUuid);
}