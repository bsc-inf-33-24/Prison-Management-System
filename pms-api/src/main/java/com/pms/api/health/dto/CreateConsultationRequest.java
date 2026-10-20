package com.pms.api.health.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateConsultationRequest(
        @NotNull UUID healthRecordId,
        @NotNull LocalDate consultationDate,
        @Size(max = 10000) String complaint,
        @Size(max = 10000) String diagnosis,
        @Size(max = 10000) String treatmentProvided
) {
}