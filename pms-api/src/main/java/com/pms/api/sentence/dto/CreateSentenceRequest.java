package com.pms.api.sentence.dto;
import com.pms.api.inmate.entity.SentenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
public record CreateSentenceRequest(@NotNull UUID inmateId, @NotBlank @Size(max=500) String offence,
        @NotNull SentenceType sentenceType, @NotNull LocalDate startDate, LocalDate endDate) { }
