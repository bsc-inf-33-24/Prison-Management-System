package com.pms.api.visitor.dto;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.visitor.entity.Visit;
import com.pms.api.visitor.entity.Visitor;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        VisitorSummary visitor,
        InmateSummary inmate,
        OffsetDateTime visitDateTime,
        Integer durationMinutes,
        UUID loggedBy,
        String recordingOfficer,
        Instant createdAt,
        Instant updatedAt
) {
    public static VisitResponse from(Visit visit) {
        return new VisitResponse(
                visit.getPublicUuid(),
                VisitorSummary.from(visit.getVisitor()),
                InmateSummary.from(visit.getInmate()),
                visit.getVisitDateTime(),
                visit.getDurationMinutes(),
                visit.getLoggedBy() != null ? visit.getLoggedBy().getPublicUuid() : null,
                visit.getLoggedBy() != null ? visit.getLoggedBy().getFullName() : null,
                visit.getCreatedAt(),
                visit.getUpdatedAt()
        );
    }

    public record VisitorSummary(UUID id, String fullName, String idNumber, String phone, String relationshipToInmate) {
        public static VisitorSummary from(Visitor visitor) {
            return new VisitorSummary(
                    visitor.getPublicUuid(),
                    visitor.getFullName(),
                    visitor.getIdNumber(),
                    visitor.getPhone(),
                    visitor.getRelationshipToInmate()
            );
        }
    }

    public record InmateSummary(UUID id, String inmateNumber, String firstName, String lastName) {
        public static InmateSummary from(Inmate inmate) {
            return new InmateSummary(
                    inmate.getPublicUuid(),
                    inmate.getInmateNumber(),
                    inmate.getFirstName(),
                    inmate.getLastName()
            );
        }
    }
}
