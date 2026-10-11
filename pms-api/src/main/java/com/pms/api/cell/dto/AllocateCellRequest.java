package com.pms.api.cell.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AllocateCellRequest(
        @NotNull(message = "cellId is required.")
        @Positive(message = "cellId must be a positive number.")
        Long cellId
) {
}
