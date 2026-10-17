package com.pms.api.cell.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record AllocateCellRequest(
        @NotNull(message = "cellId is required.")
        UUID cellId
) {
}
