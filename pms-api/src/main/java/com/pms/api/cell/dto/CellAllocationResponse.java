package com.pms.api.cell.dto;

import com.pms.api.cell.entity.CellAllocation;
import java.time.Instant;
import java.util.UUID;

public record CellAllocationResponse(
        UUID inmateId,
        Long cellId,
        Instant allocatedAt,
        Integer currentOccupancy,
        Integer availableSpaces
) {
    public static CellAllocationResponse from(CellAllocation allocation, int currentOccupancy, int availableSpaces) {
        return new CellAllocationResponse(
                allocation.getInmate().getPublicUuid(),
                allocation.getCell().getId(),
                allocation.getAllocatedAt(),
                currentOccupancy,
                availableSpaces
        );
    }
}
