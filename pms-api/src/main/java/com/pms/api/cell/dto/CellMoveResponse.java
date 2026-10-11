package com.pms.api.cell.dto;

import java.time.Instant;

public record CellMoveResponse(
        Long inmateId,
        Long oldCellId,
        Long newCellId,
        Instant allocatedAt,
        Integer oldCellOccupancy,
        Integer newCellOccupancy
) {
}
