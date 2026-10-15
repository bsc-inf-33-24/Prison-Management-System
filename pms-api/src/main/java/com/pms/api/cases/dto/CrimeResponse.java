package com.pms.api.cases.dto;

import com.pms.api.cases.entity.Crime;
import java.time.Instant;
import java.util.UUID;

public record CrimeResponse(
        UUID id,
        String offenceName,
        String description,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
    public static CrimeResponse from(Crime crime) {
        return new CrimeResponse(
                crime.getPublicUuid(),
                crime.getOffenceName(),
                crime.getDescription(),
                crime.getCategory(),
                crime.getCreatedAt(),
                crime.getUpdatedAt()
        );
    }
}
