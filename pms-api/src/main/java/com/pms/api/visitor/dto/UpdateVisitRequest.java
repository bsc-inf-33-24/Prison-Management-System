package com.pms.api.visitor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record UpdateVisitRequest(
        @NotNull(message = "startDateTime is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime startDateTime,
        @NotNull(message = "endDateTime is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime endDateTime
) {
    @AssertTrue(message = "endDateTime must not be earlier than startDateTime.")
    @JsonIgnore
    public boolean isVisitTimeRangeValid() {
        return startDateTime == null || endDateTime == null || !endDateTime.isBefore(startDateTime);
    }
}
