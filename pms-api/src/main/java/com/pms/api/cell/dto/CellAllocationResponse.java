package com.pms.api.cell.dto;

import com.pms.api.cell.entity.CellAllocation;
import java.time.Instant;

public record CellAllocationResponse(
        Long inmateId,
        Long cellId,
        Instant allocatedAt,
        Integer currentOccupancy,
        Integer availableSpaces
) {
    public static CellAllocationResponse from(CellAllocation allocation, int currentOccupancy, int availableSpaces) {
        return new CellAllocationResponse(
                allocation.getInmate().getId(),
                allocation.getCell().getId(),
                allocation.getAllocatedAt(),
                currentOccupancy,
                availableSpaces
        );
    }
}
