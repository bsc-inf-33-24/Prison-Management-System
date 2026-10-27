package com.pms.api.incident.repository;

import com.pms.api.incident.entity.IncidentInmate;
import com.pms.api.incident.entity.IncidentInmateId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentInmateRepository extends JpaRepository<IncidentInmate, IncidentInmateId> {

    // Every inmate (and their role) linked to one incident - used to build
    // IncidentResponse.participants.
    @Query("select ii from IncidentInmate ii join fetch ii.inmate where ii.id.incidentId = :incidentId")
    List<IncidentInmate> findByIncidentId(@Param("incidentId") Long incidentId);

    // Every incident one inmate was involved in, most recent first - used
    // for GET /api/v1/inmates/{id}/incidents.
    @Query("""
            select ii from IncidentInmate ii
            join fetch ii.incident
            where ii.id.inmateId = :inmateId
            order by ii.incident.incidentDate desc
            """)
    List<IncidentInmate> findByInmateIdOrderByIncidentDateDesc(@Param("inmateId") Long inmateId);
}
