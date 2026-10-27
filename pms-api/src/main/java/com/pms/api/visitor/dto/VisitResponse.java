package com.pms.api.visitor.dto;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.visitor.entity.Visit;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        VisitorSummary visitor,
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
    public static VisitResponse from(Visit visit) {
        return new VisitResponse(
                visit.getPublicUuid(),
                VisitorSummary.from(visit),
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
        public static VisitorSummary from(Visit visit) {
            return new VisitorSummary(visit.getVisitorFullName());
        }
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
