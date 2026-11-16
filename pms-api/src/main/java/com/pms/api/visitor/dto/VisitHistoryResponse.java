package com.pms.api.visitor.dto;

import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.entity.VisitType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisitHistoryResponse(
        UUID id,
        VisitType visitType,
        LocalDate visitDate,
        OffsetDateTime startDateTime,
        OffsetDateTime endDateTime,
        Integer durationMinutes,
        String visitorName,
        VisitResponse.VisitorSummary visitor,
        String legalCaseNumber,
        String legalRepresentative,
        String recordingOfficer
) {
    public static VisitHistoryResponse from(Visit visit, boolean canViewFamilyDetails,
                                            boolean canViewLegalDetails) {
        var familyDetails = canViewFamilyDetails ? visit.getFamilyDetails() : null;
        var legalDetails = canViewLegalDetails ? visit.getLegalDetails() : null;
        return new VisitHistoryResponse(
                visit.getPublicUuid(),
                visit.getVisitType(),
                visit.getStartDateTime().toLocalDate(),
                visit.getStartDateTime(),
                visit.getEndDateTime(),
                visit.getDurationMinutes(),
                familyDetails != null ? familyDetails.getVisitorFullName() : null,
                familyDetails != null ? new VisitResponse.VisitorSummary(familyDetails.getVisitorFullName()) : null,
                legalDetails != null ? legalDetails.getLegalCaseNumber() : null,
                legalDetails != null ? legalDetails.getLegalRepresentative() : null,
                visit.getLoggedBy() != null ? visit.getLoggedBy().getFullName() : null
        );
    }
}
