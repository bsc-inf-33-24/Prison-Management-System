package com.pms.api.cell.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AllocateCellRequest(
        @NotNull(message = "cellId is required.")
        @Positive(message = "cellId must be a positive database ID.")
        @Schema(description = "Numeric database ID returned as id by the cell list and detail endpoints.")
        Long cellId
) {
}
