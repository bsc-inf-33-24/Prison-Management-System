package com.pms.api.cell.dto;

import java.util.UUID;

public record CellInmateSummary(
        UUID id,
        String firstName,
        String lastName
) {
}
