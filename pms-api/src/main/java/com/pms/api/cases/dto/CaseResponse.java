package com.pms.api.cases.dto;

import com.pms.api.cases.entity.Case;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.sentence.dto.SentenceResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

public record CaseResponse(
        UUID id,
        String caseNumber,
        String court,
        String crimes,
        LocalDate caseStartDate,
        LocalDate caseEndDate,
        String status,
        Long remainingDays,
        InmateSummary inmate,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy
) {
    public static CaseResponse from(Case caseEntity) {
        return new CaseResponse(
                caseEntity.getPublicUuid(),
                caseEntity.getCaseNumber(),
                caseEntity.getCourt(),
                caseEntity.getCaseStartDate(),
                caseEntity.getCaseEndDate(),
                caseEntity.getStatus().name().toLowerCase(),
                remainingDays(caseEntity),
                InmateSummary.from(caseEntity.getInmate()),
                caseEntity.getCrimes(),
                caseEntity.getCreatedAt(),
                caseEntity.getUpdatedAt(),
                caseEntity.getCreatedBy() == null ? null : caseEntity.getCreatedBy().getPublicUuid()
        );
    }

    public static long remainingDays(Case caseEntity) {
        if (caseEntity.getCaseEndDate() == null) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), caseEntity.getCaseEndDate());
    }

    public record InmateSummary(UUID id, String inmateNumber, String firstName, String lastName) {
        public static InmateSummary from(Inmate inmate) {
            return new InmateSummary(
                    inmate.getPublicUuid(),
                    inmate.getInmateNumber(),
                    inmate.getFirstName(),
                    inmate.getLastName()
            );
        }
    }
}
