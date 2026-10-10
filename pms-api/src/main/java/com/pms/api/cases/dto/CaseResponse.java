package com.pms.api.cases.dto;

import com.pms.api.cases.entity.Case;
import com.pms.api.cases.entity.Crime;
import com.pms.api.inmate.entity.Inmate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public record CaseResponse(
        Long id,
        String caseNumber,
        String court,
        LocalDate caseStartDate,
        LocalDate caseEndDate,
        String status,
        Long remainingDays,
        InmateSummary inmate,
        List<CrimeSummary> crimes,
        List<Object> sentences,
        Instant createdAt,
        Instant updatedAt,
        Long createdBy
) {
    public static CaseResponse from(Case caseEntity) {
        return new CaseResponse(
                caseEntity.getId(),
                caseEntity.getCaseNumber(),
                caseEntity.getCourt(),
                caseEntity.getCaseStartDate(),
                caseEntity.getCaseEndDate(),
                caseEntity.getStatus().name().toLowerCase(),
                remainingDays(caseEntity),
                InmateSummary.from(caseEntity.getInmate()),
                caseEntity.getCrimes().stream().map(CrimeSummary::from).collect(Collectors.toList()),
                List.of(),
                caseEntity.getCreatedAt(),
                caseEntity.getUpdatedAt(),
                caseEntity.getCreatedBy() == null ? null : caseEntity.getCreatedBy().getId()
        );
    }

    public static long remainingDays(Case caseEntity) {
        if (caseEntity.getCaseEndDate() == null) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), caseEntity.getCaseEndDate());
    }

    public record InmateSummary(Long id, String inmateNumber, String firstName, String lastName) {
        public static InmateSummary from(Inmate inmate) {
            return new InmateSummary(
                    inmate.getId(),
                    inmate.getInmateNumber(),
                    inmate.getFirstName(),
                    inmate.getLastName()
            );
        }
    }

    public record CrimeSummary(Long id, String offenceName, String description, String category) {
        public static CrimeSummary from(Crime crime) {
            return new CrimeSummary(
                    crime.getId(),
                    crime.getOffenceName(),
                    crime.getDescription(),
                    crime.getCategory()
            );
        }
    }
}
