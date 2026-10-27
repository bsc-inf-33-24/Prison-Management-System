package com.pms.api.inmate.dto;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.entity.SentenceDurationUnit;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InmateResponse(
        UUID id,
        String inmateNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        LocalDate admissionDate,
        String offence,
        Integer sentenceDurationValue,
        SentenceDurationUnit sentenceDurationUnit,
        LocalDate sentenceStartDate,
        LocalDate sentenceEndDate,
        UUID cellId,
        Instant createdAt,
        Instant updatedAt
) {

    public static InmateResponse from(Inmate inmate) {
        return from(inmate, inmate.getCellAllocation() == null
                ? null
                : inmate.getCellAllocation().getCell().getPublicUuid());
    }

    public static InmateResponse from(Inmate inmate, UUID cellId) {
        return new InmateResponse(
                inmate.getPublicUuid(),
                inmate.getInmateNumber(),
                inmate.getFirstName(),
                inmate.getLastName(),
                inmate.getDateOfBirth(),
                inmate.getGender(),
                inmate.getAdmissionDate(),
                inmate.getOffence(),
                inmate.getSentenceDurationValue(),
                inmate.getSentenceDurationUnit(),
                inmate.getSentenceStartDate(),
                inmate.getSentenceEndDate(),
                cellId,
                inmate.getCreatedAt(),
                inmate.getUpdatedAt()
        );
    }
}
