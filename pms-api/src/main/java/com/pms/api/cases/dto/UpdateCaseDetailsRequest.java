package com.pms.api.cases.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Optional case fields; null values are left unchanged. An empty crimeIds set clears linked crimes. */
public record UpdateCaseDetailsRequest(
        @Size(min = 1, max = 80, message = "Case number must not be blank and must not exceed 80 characters.")
        String caseNumber,
        @Size(min = 1, max = 150, message = "Court must not be blank and must not exceed 150 characters.")
        String court,
        LocalDate caseStartDate,
        LocalDate caseEndDate,
        Set<UUID> crimeIds
) {
}
