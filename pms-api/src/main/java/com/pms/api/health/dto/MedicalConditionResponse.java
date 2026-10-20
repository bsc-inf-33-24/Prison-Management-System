package com.pms.api.health.dto;

import com.pms.api.health.entity.MedicalCondition;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MedicalConditionResponse(
        UUID id,
        UUID healthRecordId,
        String conditionType,
        String conditionName,
        String severity,
        LocalDate dateRecorded,
        String status,
        String recordedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static MedicalConditionResponse from(MedicalCondition condition) {
        return new MedicalConditionResponse(
                condition.getPublicUuid(),
                condition.getHealthRecord() == null
                        ? null
                        : condition.getHealthRecord().getPublicUuid(),
                condition.getConditionType(),
                condition.getConditionName(),
                condition.getSeverity(),
                condition.getDateRecorded(),
                condition.getStatus(),
                condition.getRecordedBy(),
                condition.getCreatedAt(),
                condition.getUpdatedAt()
        );
    }
}
