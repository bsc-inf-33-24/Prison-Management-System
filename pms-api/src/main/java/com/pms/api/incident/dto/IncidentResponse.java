package com.pms.api.incident.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

// What we send back to the client after logging an incident or recording
// an outcome. loggedBy is just the user's ID (not the whole User object) —
// we don't want to leak other staff details through this endpoint.
public record IncidentResponse(
        Long id,
        List<Long> inmateIds,
        OffsetDateTime incidentDate,
        String description,
        String outcome,
        Long loggedBy,
        Instant createdAt
) {
}