package com.pms.api.visitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pms.api.visitor.entity.VisitType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record VisitRequest(
        @JsonProperty("type") @JsonAlias("visitType")
        @NotNull(message = "type is required") VisitType type,
        @Size(max = 255, message = "visitorFullName must not exceed 255 characters") String visitorFullName,
        @NotNull(message = "inmateId is required") UUID inmateId,
        @NotNull(message = "visitDate is required") LocalDate visitDate,
        @NotNull(message = "startTime is required") LocalTime startTime,
        @NotNull(message = "endTime is required") LocalTime endTime,
        @Size(max = 1000, message = "itemsBrought must not exceed 1000 characters") String itemsBrought,
        @JsonProperty("sickness") @JsonAlias("complaint")
        @Size(max = 10000, message = "sickness must not exceed 10000 characters") String sickness,
        @Size(max = 10000, message = "diagnosis must not exceed 10000 characters") String diagnosis,
        @Size(max = 10000, message = "medicationsProvided must not exceed 10000 characters") String medicationsProvided,
        @Size(max = 10000, message = "recommendations must not exceed 10000 characters") String recommendations,
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
        VisitType resolvedType = type;
        return resolvedType != VisitType.FAMILY || visitorFullName != null && !visitorFullName.isBlank();
    }

    @AssertTrue(message = "sickness and diagnosis are required for health visits.")
    @JsonIgnore
    public boolean isHealthVisitDetailsValid() {
        VisitType resolvedType = type;
        if (resolvedType != VisitType.HEALTH) {
            return true;
        }
        return sickness != null && !sickness.isBlank() && diagnosis != null && !diagnosis.isBlank();
    }

    @AssertTrue(message = "legalCaseNumber is required for legal visits.")
    @JsonIgnore
    public boolean isLegalVisitDetailsValid() {
        VisitType resolvedType = type;
        if (resolvedType != VisitType.LEGAL) {
            return true;
        }
        return legalCaseNumber != null && !legalCaseNumber.isBlank();
    }
}
