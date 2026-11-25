package com.pms.api.sentence.dto;
import com.pms.api.inmate.entity.SentenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
public record CreateSentenceRequest(@NotNull UUID inmateId, @NotBlank @Size(max=500) String offence,
        SentenceType sentenceType, LocalDate startDate, LocalDate endDate,
        @Valid CreateRemissionRequest remission) {
    @AssertTrue(message = "Provide all required sentence details, or omit them all for an offence-only REMAND record.")
    @JsonIgnore
    public boolean isSentenceDetailsConsistent() {
        boolean offenceOnly = sentenceType == null && startDate == null && endDate == null && remission == null;
        boolean sentenceDetails = sentenceType != null && startDate != null;
        return offenceOnly || sentenceDetails;
    }
}
