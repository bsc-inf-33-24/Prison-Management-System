package com.pms.api.incident.dto;

import java.time.OffsetDateTime;

// A lighter shape for GET /api/inmates/{id}/incidents — a list of past
// incidents for one inmate doesn't need to repeat which inmates were
// involved (we already know: this one), so we drop that field here.
public record IncidentHistoryItemResponse(
        Long id,
        OffsetDateTime incidentDate,
        String description,
        String outcome
) {
}