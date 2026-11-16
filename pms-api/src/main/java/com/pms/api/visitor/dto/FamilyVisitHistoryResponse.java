package com.pms.api.visitor.dto;

import com.pms.api.visitor.entity.VisitType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FamilyVisitHistoryResponse(
        UUID id,
        VisitType visitType,
        LocalDate visitDate,
        OffsetDateTime startDateTime,
        OffsetDateTime endDateTime,
        Integer durationMinutes,
        String visitorName,
        VisitResponse.VisitorSummary visitor,
        String recordingOfficer
) {
    public static FamilyVisitHistoryResponse from(VisitHistoryResponse visit) {
        return new FamilyVisitHistoryResponse(
                visit.id(),
                visit.visitType(),
                visit.visitDate(),
                visit.startDateTime(),
                visit.endDateTime(),
                visit.durationMinutes(),
                visit.visitorName(),
                visit.visitor(),
                visit.recordingOfficer());
    }
}
