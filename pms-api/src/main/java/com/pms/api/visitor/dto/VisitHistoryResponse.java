package com.pms.api.visitor.dto;

import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.entity.VisitType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VisitHistoryResponse(
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
    public static VisitHistoryResponse from(Visit visit, boolean canViewFamilyDetails) {
        var familyDetails = canViewFamilyDetails ? visit.getFamilyDetails() : null;
        return new VisitHistoryResponse(
                visit.getPublicUuid(),
                visit.getVisitType(),
                visit.getStartDateTime().toLocalDate(),
                visit.getStartDateTime(),
                visit.getEndDateTime(),
                visit.getDurationMinutes(),
                familyDetails != null ? familyDetails.getVisitorFullName() : null,
                familyDetails != null ? new VisitResponse.VisitorSummary(familyDetails.getVisitorFullName()) : null,
                visit.getLoggedBy() != null ? visit.getLoggedBy().getFullName() : null
        );
    }
}
