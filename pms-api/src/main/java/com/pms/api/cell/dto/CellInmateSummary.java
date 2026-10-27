package com.pms.api.cell.dto;

import java.util.UUID;

public record CellInmateSummary(
        UUID id,
        String inmateNumber,
        String firstName,
        String lastName
) {
}
