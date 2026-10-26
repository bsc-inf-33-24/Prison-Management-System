package com.pms.api.health.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecordMedicalVisitRequest(
        @NotNull(message = "Inmate ID is required.")
        UUID inmateId,

        @NotNull(message = "Visit date is required.")
        @PastOrPresent(message = "Visit date cannot be in the future.")
        LocalDate visitDate,

        @NotBlank(message = "Sickness or presenting complaint is required.")
        @Size(max = 10000, message = "Sickness or presenting complaint must be at most 10000 characters.")
        String sickness,

        @NotBlank(message = "Diagnosis is required.")
        @Size(max = 10000, message = "Diagnosis must be at most 10000 characters.")
        String diagnosis,

        @Size(max = 10000, message = "Medications provided must be at most 10000 characters.")
        String medicationsProvided,

        @Size(max = 10000, message = "Recommendations must be at most 10000 characters.")
        String recommendations
) {
}
