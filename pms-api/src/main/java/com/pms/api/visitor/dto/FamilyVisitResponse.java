package com.pms.api.visitor.dto;

import com.pms.api.visitor.entity.VisitType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FamilyVisitResponse(
        UUID id,
        VisitType visitType,
        VisitResponse.VisitorSummary visitor,
        String itemsBrought,
        VisitResponse.InmateSummary inmate,
        LocalDate visitDate,
        OffsetDateTime startDateTime,
        OffsetDateTime endDateTime,
        Integer durationMinutes,
        UUID loggedBy,
        String recordingOfficer,
        Instant createdAt,
        Instant updatedAt
) {
    public static FamilyVisitResponse from(VisitResponse visit) {
        return new FamilyVisitResponse(
                visit.id(),
                visit.visitType(),
                visit.visitor(),
                visit.itemsBrought(),
                visit.inmate(),
                visit.visitDate(),
                visit.startDateTime(),
                visit.endDateTime(),
                visit.durationMinutes(),
                visit.loggedBy(),
                visit.recordingOfficer(),
                visit.createdAt(),
                visit.updatedAt());
    }
}
