package com.pms.api.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ApproveTransferRequest(@NotNull UUID cellId, @Size(max = 2000) String note) { }
