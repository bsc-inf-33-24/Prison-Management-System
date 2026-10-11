package com.pms.api.cell.dto;

import com.pms.api.cell.entity.Cell;
import java.util.List;

public record CellDetailResponse(
        Long id,
        String name,
        String blockName,
        Integer cellNumber,
        Integer capacity,
        String classification,
        Integer currentOccupancy,
        Integer availableSpaces,
        List<CellInmateSummary> inmates
) {
    public static CellDetailResponse from(Cell cell, int occupancy, List<CellInmateSummary> inmates) {
        return new CellDetailResponse(
                cell.getId(),
                cell.getName(),
                cell.getBlockName(),
                cell.getCellNumber(),
                cell.getCapacity(),
                cell.getClassification(),
                occupancy,
                Math.max(cell.getCapacity() - occupancy, 0),
                inmates
        );
    }
}
