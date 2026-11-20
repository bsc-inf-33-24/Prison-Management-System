package com.pms.api.sentence.dto;
import com.pms.api.inmate.entity.SentenceType;
import com.pms.api.sentence.entity.InmateSentence;
import com.pms.api.sentence.entity.SentenceStatus;
import com.pms.api.sentence.entity.SentenceRemission;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record SentenceResponse(UUID id, UUID inmateId, String offence, SentenceType sentenceType,
        LocalDate startDate, LocalDate endDate, SentenceStatus status, BigDecimal remissionPercentage,
        LocalDate projectedReleaseDate, List<RemissionResponse> remissions) {
    public static SentenceResponse from(InmateSentence s, List<SentenceRemission> remissions,
            LocalDate projectedReleaseDate) {
        return new SentenceResponse(s.getPublicUuid(), s.getInmate().getPublicUuid(), s.getOffence(),
                s.getSentenceType(), s.getStartDate(), s.getEndDate(), s.getStatus(),
                remissions.stream().map(SentenceRemission::getRemissionPercentage)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), projectedReleaseDate,
                remissions.stream().map(r -> RemissionResponse.from(r, projectedReleaseDate)).toList());
    }
}
