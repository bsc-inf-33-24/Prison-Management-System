package com.pms.api.transfer.dto;

import com.pms.api.transfer.entity.TransferStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTransferStatusRequest(@NotNull TransferStatus status) { }
