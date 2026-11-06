package com.pms.api.cell.dto;

import com.pms.api.cell.entity.Cell;
import java.util.UUID;

public record CellResponse(
        UUID id,
        UUID prisonId,
        UUID blockId,
        String blockName,
        Integer cellNumber,
        String name,
        Integer capacity,
        Integer currentOccupancy,
        Integer availableSpaces
) {
    public static CellResponse from(Cell cell, long occupancy) {
        int currentOccupancy = Math.toIntExact(occupancy);
        return new CellResponse(
                cell.getPublicUuid(),
                cell.getBlock() == null ? null : cell.getBlock().getPrison().getPublicUuid(),
                cell.getBlock() == null ? null : cell.getBlock().getPublicUuid(),
                cell.getBlockName(),
                cell.getCellNumber(),
                cell.getName(),
                cell.getCapacity(),
                currentOccupancy,
                Math.max(cell.getCapacity() - currentOccupancy, 0)
        );
    }
}
