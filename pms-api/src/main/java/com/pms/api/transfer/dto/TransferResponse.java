package com.pms.api.transfer.dto;

import com.pms.api.transfer.entity.InmateTransfer;
import com.pms.api.transfer.entity.TransferStatus;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(UUID id, UUID inmateId, String inmateName,
        UUID sourcePrisonId, String sourcePrisonName, UUID destinationPrisonId, String destinationPrisonName,
        String reason, TransferStatus status, Instant requestedAt, String requestedBy,
        Instant decidedAt, String decidedBy, String decisionNote,
        Instant dispatchedAt, String dispatchedBy, Instant receivedAt, String receivedBy,
        Instant cancelledAt, String cancelledBy) {
    public static TransferResponse from(InmateTransfer t) {
        return new TransferResponse(t.getPublicUuid(),t.getInmate().getPublicUuid(),
                t.getInmate().getFirstName()+" "+t.getInmate().getLastName(),
                t.getSourcePrison().getPublicUuid(),t.getSourcePrison().getName(),
                t.getDestinationPrison().getPublicUuid(),t.getDestinationPrison().getName(),
                t.getReason(),t.getStatus(),t.getRequestedAt(),t.getRequestedBy(),t.getDecidedAt(),
                t.getDecidedBy(),t.getDecisionNote(),t.getDispatchedAt(),t.getDispatchedBy(),
                t.getReceivedAt(),t.getReceivedBy(),t.getCancelledAt(),t.getCancelledBy());
    }
}
