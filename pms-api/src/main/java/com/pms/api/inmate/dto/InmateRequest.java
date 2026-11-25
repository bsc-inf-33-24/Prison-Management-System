package com.pms.api.inmate.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pms.api.inmate.entity.InmateType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InmateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull @Past(message = "Date of birth must be before today.") LocalDate dateOfBirth,
        @NotBlank @Size(max = 20) String gender,
        @NotNull LocalDate admissionDate,
        @NotNull InmateType inmateType,
        @NotBlank @Size(max = 10000) String bio,
        @NotBlank @Size(max = 10000) String medicalHistory,
        @Schema(description = "Required for REMAND inmates; must be omitted for SENTENCED inmates.")
        LocalDate expectedCourtDate,
        @NotBlank @Size(max = 200) String nextOfKinName,
        @NotBlank @Size(max = 100) String nextOfKinRelationship,
        @NotBlank @Size(max = 40) String nextOfKinPhone,
        @Email @Size(max = 254) String nextOfKinEmail,
        @Schema(description = "Optional place of origin for the inmate.")
        @Size(max = 255) String homeOfOrigin,
        @NotNull UUID cellId
) {
    @AssertTrue(message = "Date of birth must be before the admission date.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isBirthDateBeforeAdmission() {
        return dateOfBirth == null || admissionDate == null || dateOfBirth.isBefore(admissionDate);
    }

    @AssertTrue(message = "REMAND inmates require an expected court date; SENTENCED inmates must not have one.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isCourtDateValidForType() {
        return inmateType == null || (inmateType == InmateType.REMAND
                ? expectedCourtDate != null : expectedCourtDate == null);
    }
}
