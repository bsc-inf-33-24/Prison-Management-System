package com.pms.api.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

// ignoreUnknown = true: if the client sends an extra field we don't expect,
// we ignore it instead of failing the whole request — matches the
// convention used by the Inmate module's request DTOs.
@JsonIgnoreProperties(ignoreUnknown = true)
public record LogIncidentRequest(

        @NotEmpty(message = "At least one inmate must be linked to the incident.")
        List<Long> inmateIds,

        @NotNull(message = "Incident date is required.")
        @PastOrPresent(message = "Incident date cannot be in the future.")
        OffsetDateTime incidentDate,

        @NotBlank(message = "Description is required.")
        @Size(max = 2000, message = "Description must be at most 2000 characters.")
        String description
) {
}