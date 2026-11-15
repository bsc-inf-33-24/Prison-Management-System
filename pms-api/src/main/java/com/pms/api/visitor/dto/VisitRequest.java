package com.pms.api.visitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pms.api.visitor.entity.VisitType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record VisitRequest(
        VisitType visitType,
        @Size(max = 255, message = "visitorFullName must not exceed 255 characters") String visitorFullName,
        @NotNull(message = "inmateId is required") UUID inmateId,
        @NotNull(message = "visitDate is required") LocalDate visitDate,
        @NotNull(message = "startTime is required") LocalTime startTime,
        @NotNull(message = "endTime is required") LocalTime endTime,
        @Size(max = 1000, message = "itemsBrought must not exceed 1000 characters") String itemsBrought,
        @Size(max = 2000, message = "complaint must not exceed 2000 characters") String complaint,
        @Size(max = 2000, message = "diagnosis must not exceed 2000 characters") String diagnosis,
        @Size(max = 2000, message = "medicationsProvided must not exceed 2000 characters") String medicationsProvided,
        @Size(max = 2000, message = "recommendations must not exceed 2000 characters") String recommendations,
        @Size(max = 255, message = "legalCaseNumber must not exceed 255 characters") String legalCaseNumber,
        @Size(max = 255, message = "legalRepresentative must not exceed 255 characters") String legalRepresentative
) {
    @AssertTrue(message = "endTime must not be earlier than startTime.")
    @JsonIgnore
    public boolean isVisitTimeRangeValid() {
        return startTime == null || endTime == null || !endTime.isBefore(startTime);
    }

    @AssertTrue(message = "visitorFullName is required for family visits.")
    @JsonIgnore
    public boolean isVisitorNameValidForFamilyVisits() {
        VisitType resolvedType = visitType == null ? VisitType.FAMILY : visitType;
        return resolvedType != VisitType.FAMILY || visitorFullName != null && !visitorFullName.isBlank();
    }

    @AssertTrue(message = "complaint and diagnosis are required for health visits.")
    @JsonIgnore
    public boolean isHealthVisitDetailsValid() {
        VisitType resolvedType = visitType == null ? VisitType.FAMILY : visitType;
        if (resolvedType != VisitType.HEALTH) {
            return true;
        }
        return complaint != null && !complaint.isBlank() && diagnosis != null && !diagnosis.isBlank();
    }

    @AssertTrue(message = "legalCaseNumber is required for legal visits.")
    @JsonIgnore
    public boolean isLegalVisitDetailsValid() {
        VisitType resolvedType = visitType == null ? VisitType.FAMILY : visitType;
        if (resolvedType != VisitType.LEGAL) {
            return true;
        }
        return legalCaseNumber != null && !legalCaseNumber.isBlank();
    }
}
