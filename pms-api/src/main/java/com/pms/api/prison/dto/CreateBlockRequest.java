package com.pms.api.prison.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateBlockRequest(@NotBlank @Size(max = 50) String name,
        @NotEmpty List<@Valid CreatePrisonCellRequest> cells) { }
