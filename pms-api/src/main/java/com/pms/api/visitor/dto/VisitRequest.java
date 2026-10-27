package com.pms.api.visitor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record VisitRequest(
        @NotBlank @Size(max = 255) String visitorFullName,
        @NotNull(message = "inmateId is required") UUID inmateId,
        @NotNull(message = "visitDate is required") LocalDate visitDate,
        @NotNull(message = "startTime is required") LocalTime startTime,
        @NotNull(message = "endTime is required") LocalTime endTime
) {
    @AssertTrue(message = "endTime must not be earlier than startTime.")
    @JsonIgnore
    public boolean isVisitTimeRangeValid() {
        return startTime == null || endTime == null || !endTime.isBefore(startTime);
    }
}
