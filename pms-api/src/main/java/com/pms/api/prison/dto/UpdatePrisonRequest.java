package com.pms.api.prison.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePrisonRequest(@NotBlank @Size(max = 160) String name,
                                  @Size(max = 255) String location) { }
