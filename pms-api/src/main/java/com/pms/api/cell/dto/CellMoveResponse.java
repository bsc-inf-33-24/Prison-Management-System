package com.pms.api.cell.dto;

import java.time.Instant;
import java.util.UUID;

public record CellMoveResponse(
        UUID inmateId,
        Long oldCellId,
        Long newCellId,
        Instant allocatedAt,
        Integer oldCellOccupancy,
        Integer newCellOccupancy
) {
}
