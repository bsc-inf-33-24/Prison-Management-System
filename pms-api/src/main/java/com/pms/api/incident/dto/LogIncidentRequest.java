package com.pms.api.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

// ignoreUnknown = true: if the client sends an extra field we don't expect,
// we ignore it instead of failing the whole request - matches the
// convention used by the Inmate module's request DTOs.
@JsonIgnoreProperties(ignoreUnknown = true)
public record LogIncidentRequest(

        @NotEmpty(message = "At least one inmate must be linked to the incident.")
        @Valid
        List<Participant> participants,

        @NotNull(message = "Incident date is required.")
        @PastOrPresent(message = "Incident date cannot be in the future.")
        LocalDate incidentDate,

        @NotBlank(message = "Description is required.")
        String description
) {

    // One inmate's involvement in the incident, including their role - this
    // is what keeps role_in_incident (V5's IncidentInmate.role_in_incident)
    // from being silently discarded.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Participant(

            @NotNull(message = "Inmate ID is required.")
            @Positive(message = "Inmate ID must be a positive number.")
            Long inmateId,

            @Size(max = 255, message = "Role in incident must be at most 255 characters.")
            String roleInIncident
    ) {
    }
}
