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
        @NotBlank(message = "inmate first name is missing or invalid")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "inmate last name is missing or invalid")
        @Size(max = 100)
        String lastName,

        @NotNull(message = "inmate date of birth is missing or is invalid")
        @Past
        LocalDate dateOfBirth,

        @NotBlank(message = "inmates gender is missing or invalid")
        @Size(max = 20)
        String gender,

        @NotNull(message = "admission date is required")
        LocalDate admissionDate,

	@NotBlank(message = "inmates offence is missing or invalid")
        @Size(min = 1, max = 500)
        String offence,

        @NotNull(message = "sentence start date is required")
        LocalDate sentenceStartDate,

        @NotNull(message = "sentence end date is required")
        LocalDate sentenceEndDate,

        @NotNull(message = "cell Id is required")
        UUID cellId
) {
    @AssertTrue(message = "Sentence end date must be on or after the start date.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isSentenceDurationComplete() {
        return sentenceStartDate == null || sentenceEndDate == null || !sentenceEndDate.isBefore(sentenceStartDate);
    }
}
