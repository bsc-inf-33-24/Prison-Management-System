package com.pms.api.health.dto;

import com.pms.api.health.entity.Consultation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConsultationResponse(
        UUID id,
        UUID healthRecordId,
        LocalDate consultationDate,
        String complaint,
        String diagnosis,
        String treatmentProvided,
        String consultedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConsultationResponse from(Consultation consultation) {
        return new ConsultationResponse(
                consultation.getPublicUuid(),
                consultation.getHealthRecord() == null
                        ? null
                        : consultation.getHealthRecord().getPublicUuid(),
                consultation.getConsultationDate(),
                consultation.getComplaint(),
                consultation.getDiagnosis(),
                consultation.getTreatmentProvided(),
                consultation.getConsultedBy(),
                consultation.getCreatedAt(),
                consultation.getUpdatedAt()
        );
    }
}