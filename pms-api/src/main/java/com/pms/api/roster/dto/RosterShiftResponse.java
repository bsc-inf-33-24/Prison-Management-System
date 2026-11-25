package com.pms.api.roster.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pms.api.roster.entity.RosterShift;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

public record RosterShiftResponse(
        UUID id,
        UUID rosterId,
        UUID officerId,
        String officerName,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String location,
        Instant createdAt,
        Instant updatedAt
) {
    public static RosterShiftResponse from(RosterShift shift, ZoneId rosterTimeZone) {
        var start = shift.getStartAt().atZoneSameInstant(rosterTimeZone);
        var end = shift.getEndAt().atZoneSameInstant(rosterTimeZone);
        return new RosterShiftResponse(
                shift.getPublicUuid(),
                shift.getRoster().getPublicUuid(),
                shift.getAssignedOfficer().getPublicUuid(),
                shift.getAssignedOfficer().getFullName(),
                start.toLocalDate(),
                start.toLocalTime(),
                end.toLocalTime(),
                shift.getLocation(),
                shift.getCreatedAt(),
                shift.getUpdatedAt());
    }
}
