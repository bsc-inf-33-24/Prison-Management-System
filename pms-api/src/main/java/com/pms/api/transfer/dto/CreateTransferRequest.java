package com.pms.api.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateTransferRequest(@NotNull UUID inmateId, @NotNull UUID destinationPrisonId,
                                    @NotBlank @Size(max=2000) String reason) { }
