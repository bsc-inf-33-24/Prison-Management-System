package com.pms.api.incident.repository;

import com.pms.api.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByPublicUuid(UUID publicUuid);
}
