package com.pms.api.sentence.dto;
import com.pms.api.sentence.entity.SentenceRemission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
public record RemissionResponse(UUID id, BigDecimal remissionPercentage, LocalDate effectiveDate,
        String reason, String approvedBy, LocalDate projectedReleaseDate) {
    public static RemissionResponse from(SentenceRemission r, LocalDate projectedReleaseDate) {
        return new RemissionResponse(r.getPublicUuid(), r.getRemissionPercentage(), r.getEffectiveDate(),
                r.getReason(), r.getApprovedBy() == null ? null : r.getApprovedBy().getFullName(),
                projectedReleaseDate);
    }
}
