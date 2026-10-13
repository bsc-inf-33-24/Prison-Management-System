package com.pms.api.visitor.dto;

import com.pms.api.visitor.entity.Visit;
import java.time.OffsetDateTime;

public record VisitHistoryResponse(
        Long id,
        OffsetDateTime visitDateTime,
        Integer durationMinutes,
        VisitResponse.VisitorSummary visitor,
        String recordingOfficer
) {
    public static VisitHistoryResponse from(Visit visit) {
        return new VisitHistoryResponse(
                visit.getId(),
                visit.getVisitDateTime(),
                visit.getDurationMinutes(),
                VisitResponse.VisitorSummary.from(visit.getVisitor()),
                visit.getLoggedBy() != null ? visit.getLoggedBy().getFullName() : null
        );
    }
}
