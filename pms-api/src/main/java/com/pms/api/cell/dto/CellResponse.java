package com.pms.api.cell.dto;

import com.pms.api.cell.entity.Cell;

public record CellResponse(
        Long id,
        String name,
        Integer capacity,
        Integer currentOccupancy,
        Integer availableSpaces
) {
    public static CellResponse from(Cell cell, long occupancy) {
        int currentOccupancy = Math.toIntExact(occupancy);
        return new CellResponse(
                cell.getId(),
                cell.getName(),
                cell.getCapacity(),
                currentOccupancy,
                Math.max(cell.getCapacity() - currentOccupancy, 0)
        );
    }
}
