package com.pms.api.visitor.dto;

import com.pms.api.visitor.entity.Visit;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VisitHistoryResponse(
        UUID id,
        LocalDate visitDate,
        OffsetDateTime startDateTime,
        OffsetDateTime endDateTime,
        Integer durationMinutes,
        String visitorName,
        VisitResponse.VisitorSummary visitor,
        String recordingOfficer
) {
    public static VisitHistoryResponse from(Visit visit) {
        return new VisitHistoryResponse(
                visit.getPublicUuid(),
                visit.getStartDateTime().toLocalDate(),
                visit.getStartDateTime(),
                visit.getEndDateTime(),
                visit.getDurationMinutes(),
                visit.getVisitorFullName(),
                VisitResponse.VisitorSummary.from(visit),
                visit.getLoggedBy() != null ? visit.getLoggedBy().getFullName() : null
        );
    }
}
