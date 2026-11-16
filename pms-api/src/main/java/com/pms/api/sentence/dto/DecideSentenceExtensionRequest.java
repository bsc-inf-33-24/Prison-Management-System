package com.pms.api.sentence.dto;

import com.pms.api.sentence.entity.SentenceExtensionDecision;
import jakarta.validation.constraints.NotNull;

public record DecideSentenceExtensionRequest(@NotNull SentenceExtensionDecision decision) { }
