package com.pms.api.roster.dto;

import com.pms.api.roster.entity.Roster;
import com.pms.api.roster.entity.RosterShift;
import com.pms.api.roster.entity.RosterStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public record RosterResponse(
        UUID id,
        UUID prisonId,
        LocalDate periodStart,
        LocalDate periodEnd,
        RosterStatus status,
        UUID createdById,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        List<RosterShiftResponse> shifts
) {
    public static RosterResponse from(Roster roster, List<RosterShift> shifts, ZoneId rosterTimeZone) {
        return new RosterResponse(
                roster.getPublicUuid(),
                roster.getPrison().getPublicUuid(),
                roster.getPeriodStart(),
                roster.getPeriodEnd(),
                roster.getStatus(),
                roster.getCreatedBy().getPublicUuid(),
                roster.getCreatedAt(),
                roster.getUpdatedAt(),
                roster.getPublishedAt(),
                shifts.stream().map(shift -> RosterShiftResponse.from(shift, rosterTimeZone)).toList());
    }
}
