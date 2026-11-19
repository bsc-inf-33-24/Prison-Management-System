package com.pms.api.transfer.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConfirmTransferReceiptRequest(@NotNull UUID cellId) { }
