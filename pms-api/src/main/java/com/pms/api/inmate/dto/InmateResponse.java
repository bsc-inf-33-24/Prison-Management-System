package com.pms.api.inmate.dto;

import com.pms.api.inmate.entity.Inmate;
import java.time.Instant;
import java.time.LocalDate;

public record InmateResponse(
        Long id,
        String inmateNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        LocalDate admissionDate,
        String classification,
        String currentFacility,
        Instant createdAt,
        Instant updatedAt
) {

    public static InmateResponse from(Inmate inmate) {
        return new InmateResponse(
                inmate.getId(),
                inmate.getInmateNumber(),
                inmate.getFirstName(),
                inmate.getLastName(),
                inmate.getDateOfBirth(),
                inmate.getGender(),
                inmate.getAdmissionDate(),
                inmate.getClassification(),
                inmate.getCurrentFacility(),
                inmate.getCreatedAt(),
                inmate.getUpdatedAt()
        );
    }
}
