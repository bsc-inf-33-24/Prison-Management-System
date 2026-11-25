package com.pms.api.roster.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateRosterRequest(
        UUID prisonId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        List<@Valid CreateShiftRequest> shifts
) {
    @AssertTrue(message = "periodEnd must be on or after periodStart.")
    @JsonIgnore
    public boolean isPeriodRangeValid() {
        return periodStart == null || periodEnd == null || !periodEnd.isBefore(periodStart);
    }
}
