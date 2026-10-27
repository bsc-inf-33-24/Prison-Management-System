package com.pms.api.cell.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCellRequest(
        @NotBlank(message = "blockName is required.")
        String blockName,
        @NotNull(message = "cellNumber is required.")
        @Positive(message = "cellNumber must be a positive number.")
        Integer cellNumber,
        @NotNull(message = "capacity is required.")
        @Positive(message = "capacity must be a positive number.")
        Integer capacity
) {
}
