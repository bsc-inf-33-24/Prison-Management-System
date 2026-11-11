package com.pms.api.roster.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateRosterRequest(
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd
) {
    @AssertTrue(message = "periodEnd must be on or after periodStart.")
    @JsonIgnore
    public boolean isPeriodRangeValid() {
        return periodStart == null || periodEnd == null || !periodEnd.isBefore(periodStart);
    }
}
