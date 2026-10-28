package com.pms.api.inmate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** All fields are optional; omitted or null fields are left unchanged. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateInmateDetailsRequest(
        @Size(min = 1, max = 100, message = "First name must not be blank and must not exceed 100 characters.")
        String firstName,

        @Size(min = 1, max = 100, message = "Last name must not be blank and must not exceed 100 characters.")
        String lastName,

        @Past(message = "Date of birth must be in the past.")
        LocalDate dateOfBirth,

        @Size(min = 1, max = 20, message = "Gender must not be blank and must not exceed 20 characters.")
        String gender,

        LocalDate admissionDate
) {
}
