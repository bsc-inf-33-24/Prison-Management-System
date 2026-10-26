package com.pms.api.cell.dto;

import com.pms.api.cell.entity.Cell;
import java.util.List;
import java.util.UUID;

public record CellDetailResponse(
        UUID id,
        String name,
        String blockName,
        Integer cellNumber,
        Integer capacity,
        Integer currentOccupancy,
        Integer availableSpaces,
        List<CellInmateSummary> inmates
) {
    public static CellDetailResponse from(Cell cell, int occupancy, List<CellInmateSummary> inmates) {
        return new CellDetailResponse(
                cell.getPublicUuid(),
                cell.getName(),
                cell.getBlockName(),
                cell.getCellNumber(),
                cell.getCapacity(),
                occupancy,
                Math.max(cell.getCapacity() - occupancy, 0),
                inmates
        );
    }
}
