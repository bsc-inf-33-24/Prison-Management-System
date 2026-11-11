package com.pms.api.roster.dto;

import com.pms.api.roster.entity.Roster;
import com.pms.api.roster.entity.RosterStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RosterSummaryResponse(
        UUID id,
        UUID prisonId,
        LocalDate periodStart,
        LocalDate periodEnd,
        RosterStatus status,
        UUID createdById,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt
) {
    public static RosterSummaryResponse from(Roster roster) {
        return new RosterSummaryResponse(
                roster.getPublicUuid(),
                roster.getPrison().getPublicUuid(),
                roster.getPeriodStart(),
                roster.getPeriodEnd(),
                roster.getStatus(),
                roster.getCreatedBy().getPublicUuid(),
                roster.getCreatedAt(),
                roster.getUpdatedAt(),
                roster.getPublishedAt());
    }
}
