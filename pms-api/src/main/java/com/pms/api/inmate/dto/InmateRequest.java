package com.pms.api.inmate.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InmateRequest(
        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotNull
        @Past
        LocalDate dateOfBirth,

        @NotBlank
        @Size(max = 20)
        String gender,

        @NotNull
        LocalDate admissionDate,

        @Size(min = 1, max = 500)
        String offence,

        @NotNull
        LocalDate sentenceStartDate,

        @NotNull
        LocalDate sentenceEndDate,

        @NotNull
        UUID cellId
) {
    @AssertTrue(message = "Sentence end date must be on or after the start date.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isSentenceDurationComplete() {
        return sentenceStartDate == null || sentenceEndDate == null || !sentenceEndDate.isBefore(sentenceStartDate);
    }
}
