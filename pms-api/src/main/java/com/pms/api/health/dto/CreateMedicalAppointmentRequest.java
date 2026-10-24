package com.pms.api.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateMedicalAppointmentRequest(
        @NotNull UUID healthRecordId,
        @NotNull LocalDate appointmentDate,
        @NotBlank @Size(max = 255) String purpose,
        @NotBlank @Size(max = 100) String appointmentType,
        @Size(max = 255) String externalFacility,
        @Size(max = 50) String status
) {
}