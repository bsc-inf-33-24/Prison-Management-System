package com.pms.api.incident.repository;

import com.pms.api.incident.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // Used for GET /api/inmates/{id}/incidents — every incident this inmate
    // was involved in, most recent first.
    @Query("SELECT i FROM Incident i JOIN i.inmates inm WHERE inm.id = :inmateId ORDER BY i.incidentDate DESC")
    List<Incident> findByInmateId(@Param("inmateId") Long inmateId);
}