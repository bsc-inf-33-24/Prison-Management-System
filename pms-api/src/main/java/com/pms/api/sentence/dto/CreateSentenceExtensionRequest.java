package com.pms.api.sentence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateSentenceExtensionRequest(@NotNull LocalDate proposedEndDate, @NotBlank String reason) { }
