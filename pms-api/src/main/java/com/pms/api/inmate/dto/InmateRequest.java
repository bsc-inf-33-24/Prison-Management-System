package com.pms.api.inmate.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pms.api.inmate.entity.SentenceDurationUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InmateRequest(
        @NotBlank
        @Size(max = 30)
        String inmateNumber,

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

        @NotBlank
        @Size(max = 50)
        String classification,

        @Size(min = 1, max = 500)
        String offence,

        @Positive
        Integer sentenceDurationValue,

        SentenceDurationUnit sentenceDurationUnit,

        LocalDate sentenceStartDate,

        @NotNull
        @Positive
        Long cellId
) {
    @AssertTrue(message = "Sentence duration and unit must be supplied together.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isSentenceDurationComplete() {
        return (sentenceDurationValue == null) == (sentenceDurationUnit == null);
    }
}
