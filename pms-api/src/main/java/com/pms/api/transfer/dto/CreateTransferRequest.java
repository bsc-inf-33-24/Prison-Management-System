package com.pms.api.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateTransferRequest(UUID inmateId, UUID sourcePrisonId, UUID destinationPrisonId,
                                    @NotBlank @Size(max=2000) String reason) { }
