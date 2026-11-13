package com.pms.api.sentence.dto;
import com.pms.api.inmate.entity.SentenceType;
import com.pms.api.sentence.entity.InmateSentence;
import com.pms.api.sentence.entity.SentenceStatus;
import java.time.LocalDate;
import java.util.UUID;
public record SentenceResponse(UUID id, UUID inmateId, String offence, SentenceType sentenceType,
        LocalDate startDate, LocalDate endDate, SentenceStatus status) {
    public static SentenceResponse from(InmateSentence s) {
        return new SentenceResponse(s.getPublicUuid(), s.getInmate().getPublicUuid(), s.getOffence(),
                s.getSentenceType(), s.getStartDate(), s.getEndDate(), s.getStatus());
    }
}
