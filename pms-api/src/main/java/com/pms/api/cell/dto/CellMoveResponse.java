package com.pms.api.cell.dto;

import java.time.Instant;
import java.util.UUID;

public record CellMoveResponse(
        UUID inmateId,
        UUID oldCellId,
        UUID newCellId,
        Instant allocatedAt,
        Integer oldCellOccupancy,
        Integer newCellOccupancy
) {
}
