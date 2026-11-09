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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import com.pms.api.inmate.entity.SentenceType;

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

        @NotNull SentenceType sentenceType,

        @Schema(description = "Original end date for a fixed-term sentence. Must be omitted for LIFE and UNDETERMINED sentences.")
        LocalDate sentenceEndDate,

        @DecimalMin("0.0") @DecimalMax("100.0") @Digits(integer = 3, fraction = 2)
        @Schema(description = "Optional remission percentage for a fixed-term sentence (0 to 100). Applied as a reduction to the original sentence duration to calculate the projected release date. Ignored for LIFE and UNDETERMINED sentences.")
        java.math.BigDecimal remissionPercentage,

        @NotNull(message = "Prison Id is required")
        UUID prisonId,

        @NotNull(message = "cell Id is required")
        UUID cellId
) {
    @AssertTrue(message = "Fixed-term sentences require an end date on or after the start date; LIFE and UNDETERMINED sentences must not have an end date or remission percentage.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isSentenceDurationComplete() {
        if (sentenceType == null) return false;
        if (sentenceType == SentenceType.FIXED_TERM) {
            return sentenceStartDate != null && sentenceEndDate != null && !sentenceEndDate.isBefore(sentenceStartDate);
        }
        return sentenceEndDate == null && remissionPercentage == null;
    }
}
