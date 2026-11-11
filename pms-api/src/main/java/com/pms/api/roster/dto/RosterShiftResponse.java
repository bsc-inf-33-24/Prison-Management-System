package com.pms.api.roster.dto;

import com.pms.api.roster.entity.RosterShift;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RosterShiftResponse(
        UUID id,
        UUID rosterId,
        UUID officerId,
        String officerName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String location,
        Instant createdAt,
        Instant updatedAt
) {
    public static RosterShiftResponse from(RosterShift shift) {
        return new RosterShiftResponse(
                shift.getPublicUuid(),
                shift.getRoster().getPublicUuid(),
                shift.getAssignedOfficer().getPublicUuid(),
                shift.getAssignedOfficer().getFullName(),
                shift.getStartAt(),
                shift.getEndAt(),
                shift.getLocation(),
                shift.getCreatedAt(),
                shift.getUpdatedAt());
    }
}
