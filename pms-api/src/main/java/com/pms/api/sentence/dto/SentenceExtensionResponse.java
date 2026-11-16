package com.pms.api.sentence.dto;

import com.pms.api.sentence.entity.SentenceExtensionRequest;
import com.pms.api.sentence.entity.SentenceExtensionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SentenceExtensionResponse(UUID id, LocalDate originalEndDate, LocalDate proposedEndDate,
        String reason, String requester, String reviewer, SentenceExtensionStatus decision,
        Instant requestedAt, Instant decidedAt) {
    public static SentenceExtensionResponse from(SentenceExtensionRequest request) {
        return new SentenceExtensionResponse(request.getPublicUuid(), request.getOriginalEndDate(),
                request.getProposedEndDate(), request.getReason(), request.getRequester().getFullName(),
                request.getReviewer() == null ? null : request.getReviewer().getFullName(),
                request.getDecision(), request.getRequestedAt(), request.getDecidedAt());
    }
}
