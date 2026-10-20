package com.pms.api.health.dto;

import com.pms.api.health.entity.MedicalAppointment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicalAppointmentResponse(
        UUID id,
        UUID healthRecordId,
        LocalDate appointmentDate,
        String purpose,
        String appointmentType,
        String externalFacility,
        String status,
        String scheduledBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static MedicalAppointmentResponse from(
            MedicalAppointment appointment) {
        return new MedicalAppointmentResponse(
                appointment.getPublicUuid(),
                appointment.getHealthRecord() == null
                        ? null
                        : appointment.getHealthRecord().getPublicUuid(),
                appointment.getAppointmentDate(),
                appointment.getPurpose(),
                appointment.getAppointmentType(),
                appointment.getExternalFacility(),
                appointment.getStatus(),
                appointment.getScheduledBy(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}