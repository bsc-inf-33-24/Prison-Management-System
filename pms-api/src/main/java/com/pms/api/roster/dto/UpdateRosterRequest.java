package com.pms.api.roster.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.pms.api.roster.entity.RosterStatus;

public record UpdateRosterRequest(
        LocalDate periodStart,
        LocalDate periodEnd,
        List<@Valid UpdateShiftRequest> shifts,
        List<UUID> removeShiftIds,
        RosterStatus status
) {
    @AssertTrue(message = "periodStart and periodEnd must be supplied together, with periodEnd on or after periodStart.")
    @JsonIgnore
    public boolean isPeriodRangeValid() {
        return (periodStart == null && periodEnd == null)
                || (periodStart != null && periodEnd != null && !periodEnd.isBefore(periodStart));
    }
}
