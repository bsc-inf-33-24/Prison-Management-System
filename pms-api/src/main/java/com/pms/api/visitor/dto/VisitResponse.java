package com.pms.api.visitor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.entity.VisitType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisitResponse(
        UUID id,
        VisitType visitType,
        VisitorSummary visitor,
        String itemsBrought,
        String complaint,
        String diagnosis,
        String medicationsProvided,
        String recommendations,
        String legalCaseNumber,
        String legalRepresentative,
        InmateSummary inmate,
        LocalDate visitDate,
        OffsetDateTime startDateTime,
        OffsetDateTime endDateTime,
        Integer durationMinutes,
        UUID loggedBy,
        String recordingOfficer,
        Instant createdAt,
        Instant updatedAt
) {
    public static VisitResponse from(Visit visit, boolean canViewFamilyDetails,
                                     boolean canViewLegalDetails, boolean canViewHealthDetails) {
        var familyDetails = visit.getFamilyDetails();
        var legalDetails = canViewLegalDetails ? visit.getLegalDetails() : null;
        var healthDetails = canViewHealthDetails ? visit.getHealthDetails() : null;
        return new VisitResponse(
                visit.getPublicUuid(),
                visit.getVisitType(),
                canViewFamilyDetails && familyDetails != null
                        ? new VisitorSummary(familyDetails.getVisitorFullName()) : null,
                canViewFamilyDetails && familyDetails != null ? familyDetails.getItemsBrought() : null,
                healthDetails != null ? healthDetails.getComplaint() : null,
                healthDetails != null ? healthDetails.getDiagnosis() : null,
                healthDetails != null ? healthDetails.getMedicationsProvided() : null,
                healthDetails != null ? healthDetails.getRecommendations() : null,
                legalDetails != null ? legalDetails.getLegalCaseNumber() : null,
                legalDetails != null ? legalDetails.getLegalRepresentative() : null,
                InmateSummary.from(visit.getInmate()),
                visit.getStartDateTime().toLocalDate(),
                visit.getStartDateTime(),
                visit.getEndDateTime(),
                visit.getDurationMinutes(),
                visit.getLoggedBy() != null ? visit.getLoggedBy().getPublicUuid() : null,
                visit.getLoggedBy() != null ? visit.getLoggedBy().getFullName() : null,
                visit.getCreatedAt(),
                visit.getUpdatedAt()
        );
    }

    public record VisitorSummary(String fullName) {
    }

    public record InmateSummary(UUID id, String firstName, String lastName) {
        public static InmateSummary from(Inmate inmate) {
            return new InmateSummary(
                    inmate.getPublicUuid(),
                    inmate.getFirstName(),
                    inmate.getLastName()
            );
        }
    }
}
