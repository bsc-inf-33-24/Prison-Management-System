package com.pms.api.prison.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateBlockRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(min = 1) List<@Valid CreatePrisonCellRequest> cells) {
}
