package com.pms.api.health.dto;

import com.pms.api.health.entity.Allergy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AllergyResponse(
        UUID id,
        UUID inmateId,
        String allergen,
        String reaction,
        String severity,
        LocalDate recordedDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AllergyResponse from(Allergy allergy) {
        return new AllergyResponse(
                allergy.getPublicUuid(),
                allergy.getInmate().getPublicUuid(),
                allergy.getAllergen(),
                allergy.getReaction(),
                allergy.getSeverity(),
                allergy.getRecordedDate(),
                allergy.getStatus(),
                allergy.getCreatedAt(),
                allergy.getUpdatedAt()
        );
    }
}