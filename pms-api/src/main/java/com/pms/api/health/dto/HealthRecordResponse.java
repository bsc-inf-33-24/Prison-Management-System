package com.pms.api.health.dto;

import com.pms.api.health.entity.HealthRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HealthRecordResponse(
        UUID id,
        UUID inmateId,
        String inmateNumber,
        LocalDate screeningDate,
        String screeningResults,
        String bloodGroup,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static HealthRecordResponse from(HealthRecord record) {
        return new HealthRecordResponse(
                record.getPublicUuid(),
                record.getInmate() == null ? null : record.getInmate().getPublicUuid(),
                record.getInmate() == null ? null : record.getInmate().getInmateNumber(),
                record.getScreeningDate(),
                record.getScreeningResults(),
                record.getBloodGroup(),
                record.getStatus(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
