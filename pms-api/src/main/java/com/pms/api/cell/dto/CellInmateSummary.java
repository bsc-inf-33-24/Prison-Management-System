package com.pms.api.cell.dto;

public record CellInmateSummary(
        Long id,
        String inmateNumber,
        String firstName,
        String lastName
) {
}
