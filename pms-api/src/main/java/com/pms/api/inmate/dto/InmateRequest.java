package com.pms.api.inmate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
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
        String classification
) {
}
