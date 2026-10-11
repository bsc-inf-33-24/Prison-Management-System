package com.pms.api.sentence.dto;

import com.pms.api.sentence.entity.Sentence;
import java.time.Instant;
import java.time.LocalDate;

public record SentenceResponse(
        Long id,
        Long caseId,
        Long inmateId,
        Integer duration,
        LocalDate startDate,
        LocalDate releaseDate,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static SentenceResponse from(Sentence sentence) {
        return new SentenceResponse(
                sentence.getId(),
                sentence.getCaseId(),
                sentence.getInmateId(),
                sentence.getDuration(),
                sentence.getStartDate(),
                sentence.getReleaseDate(),
                sentence.getStatus(),
                sentence.getCreatedAt(),
                sentence.getUpdatedAt()
        );
    }
}
