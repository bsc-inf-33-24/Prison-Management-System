package com.pms.api.prison.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePrisonCellRequest(@NotNull @Positive Integer cellNumber,
                                      @NotNull @Positive Integer capacity) { }
