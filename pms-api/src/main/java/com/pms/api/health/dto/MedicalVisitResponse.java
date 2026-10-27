package com.pms.api.health.dto;

import com.pms.api.health.entity.MedicalVisit;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicalVisitResponse(
        UUID id,
        LocalDate visitDate,
        String sickness,
        String diagnosis,
        String medicationsProvided,
        String recommendations,
        String recordedBy,
        Instant recordedAt
) {

    public static MedicalVisitResponse from(MedicalVisit visit) {
        return new MedicalVisitResponse(
                visit.getPublicUuid(),
                visit.getVisitDate(),
                visit.getSickness(),
                visit.getDiagnosis(),
                visit.getMedicationsProvided(),
                visit.getRecommendations(),
                visit.getRecordedBy().getFullName(),
                visit.getCreatedAt());
    }
}
