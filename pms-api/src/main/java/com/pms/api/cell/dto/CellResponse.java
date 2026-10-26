package com.pms.api.cell.dto;

import com.pms.api.cell.entity.Cell;
import io.swagger.v3.oas.annotations.media.Schema;

public record CellResponse(
        @Schema(description = "Numeric database ID used as cellId in allocation requests and cell lookup paths.")
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
