package com.pms.api.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("SELECT i FROM Incident i JOIN i.inmates inm WHERE inm.id = :inmateId ORDER BY i.incidentDate DESC")
    List<Incident> findByInmateId(@Param("inmateId") Long inmateId);
}
