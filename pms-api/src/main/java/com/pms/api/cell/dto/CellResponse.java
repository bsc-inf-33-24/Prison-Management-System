package com.pms.api.cell.dto;

import com.pms.api.cell.entity.Cell;
import java.util.UUID;

public record CellResponse(
        UUID id,
        String name,
        Integer capacity,
        Integer currentOccupancy,
        Integer availableSpaces
) {
    public static CellResponse from(Cell cell, long occupancy) {
        int currentOccupancy = Math.toIntExact(occupancy);
        return new CellResponse(
                cell.getPublicUuid(),
                cell.getName(),
                cell.getCapacity(),
                currentOccupancy,
                Math.max(cell.getCapacity() - currentOccupancy, 0)
        );
    }
}
