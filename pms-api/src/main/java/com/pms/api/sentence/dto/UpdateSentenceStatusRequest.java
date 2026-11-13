package com.pms.api.sentence.dto;
import com.pms.api.sentence.entity.SentenceStatus;
import jakarta.validation.constraints.NotNull;
public record UpdateSentenceStatusRequest(@NotNull SentenceStatus status) { }
