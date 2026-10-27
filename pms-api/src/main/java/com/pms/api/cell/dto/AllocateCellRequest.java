package com.pms.api.cell.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AllocateCellRequest(
        @NotNull(message = "cellId is required.")
        @Schema(description = "Cell UUID returned as id by the cell list and detail endpoints.")
        UUID cellId
) {
}
