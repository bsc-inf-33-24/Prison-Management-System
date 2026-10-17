package com.pms.api.incident.repository;

import com.pms.api.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByPublicUuid(UUID publicUuid);

    // Atomic, concurrency-safe outcome recording. This UPDATE only matches a
    // row (and returns 1) if the incident is still "open" at the moment it
    // runs. If two requests race to record an outcome on the same incident,
    // the database guarantees only one UPDATE's WHERE clause matches - the
    // loser affects 0 rows, which the service turns into 409 Conflict.
    // No @Version column or SELECT ... FOR UPDATE needed; this is a plain
    // conditional UPDATE, which is safe because the WHERE clause is
    // evaluated against the latest committed row at UPDATE time, not against
    // a value read earlier in the transaction.
    @Modifying
    @Transactional
    @Query("""
            update Incident i
               set i.outcome = :outcome, i.status = :resolvedStatus
             where i.id = :incidentId
               and i.status = :openStatus
            """)
    int recordOutcomeIfOpen(@Param("incidentId") Long incidentId,
                             @Param("outcome") String outcome,
                             @Param("resolvedStatus") String resolvedStatus,
                             @Param("openStatus") String openStatus);
}
