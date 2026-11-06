package com.pms.api.prison.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePrisonRequest(@NotBlank @Size(max = 160) String name,
        @Size(max = 255) String location,
        @NotEmpty List<@Valid CreateBlockRequest> blocks) { }
