package com.pms.api.health.dto;

import com.pms.api.health.entity.Medication;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicationResponse(
        UUID id,
        UUID consultationId,
        String drugName,
        String dosage,
        String frequency,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String prescribedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static MedicationResponse from(Medication medication) {
        return new MedicationResponse(
                medication.getPublicUuid(),
                medication.getConsultation() == null ? null
                        : medication.getConsultation().getPublicUuid(),
                medication.getDrugName(),
                medication.getDosage(),
                medication.getFrequency(),
                medication.getStartDate(),
                medication.getEndDate(),
                medication.getStatus(),
                medication.getPrescribedBy(),
                medication.getCreatedAt(),
                medication.getUpdatedAt()
        );
    }
}