package com.pms.api.inmate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.util.UUID;

public record ReadmissionRequest(@NotNull UUID cellId, @NotNull @PastOrPresent LocalDate admissionDate) { }
