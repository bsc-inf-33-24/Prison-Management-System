package com.pms.api.incident.dto;

import java.time.LocalDate;
import java.util.UUID;

// A lighter shape for GET /api/inmates/{id}/incidents - a list of past
// incidents for one inmate. roleInIncident is that inmate's own role in
// this particular incident (from IncidentInmate.role_in_incident), not a
// list of everyone else's roles - we already know which inmate this is.
public record IncidentHistoryItemResponse(
        UUID id,
        LocalDate incidentDate,
        String description,
        String status,
        String outcome,
        String roleInIncident
) {
}
