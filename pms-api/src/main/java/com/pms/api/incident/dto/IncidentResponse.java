package com.pms.api.incident.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// What we send back after logging an incident or recording an outcome.
// reportedBy is the reporting officer's name (a plain string in the schema,
// not a user ID - see Incident.reportedBy). Every field here traces back to
// an actual V5 column; nothing is invented.
public record IncidentResponse(
        UUID id,
        LocalDate incidentDate,
        String description,
        String status,
        String outcome,
        String reportedBy,
        List<ParticipantResponse> participants
) {
        public record ParticipantResponse(UUID inmateId, String roleInIncident) {
    }
}
