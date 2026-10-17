package com.pms.api.sentence.dto;

import com.pms.api.sentence.entity.Sentence;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SentenceResponse(
        UUID id,
        UUID caseId,
        UUID inmateId,
        Integer duration,
        LocalDate startDate,
        LocalDate releaseDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static SentenceResponse from(Sentence sentence) {
        return new SentenceResponse(
                sentence.getPublicUuid(),
                sentence.getCase() == null ? null : sentence.getCase().getPublicUuid(),
                sentence.getInmate() == null ? null : sentence.getInmate().getPublicUuid(),
                sentence.getDuration(),
                sentence.getStartDate(),
                sentence.getReleaseDate(),
                sentence.getStatus(),
                sentence.getCreatedAt(),
                sentence.getUpdatedAt()
        );
    }
}
