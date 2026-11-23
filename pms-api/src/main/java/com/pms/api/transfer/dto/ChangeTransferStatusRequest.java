package com.pms.api.transfer.dto;

import com.pms.api.transfer.entity.TransferStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeTransferStatusRequest(@NotNull TransferStatus status,
                                          UUID destinationPrisonId,
                                          UUID cellId) {
    public ChangeTransferStatusRequest(TransferStatus status, UUID destinationPrisonId) {
        this(status, destinationPrisonId, null);
    }
}
