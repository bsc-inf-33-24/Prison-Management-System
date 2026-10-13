package com.pms.api.visitor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record VisitRequest(
        @NotNull(message = "visitorId is required") Long visitorId,
        @NotNull(message = "inmateId is required") Long inmateId,
        @NotNull(message = "visitDateTime is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime visitDateTime,
        @NotNull(message = "durationMinutes is required")
        @Min(value = 0, message = "durationMinutes cannot be negative")
        @Max(value = 1440, message = "durationMinutes exceeds the supported maximum") Integer durationMinutes
) {
}
