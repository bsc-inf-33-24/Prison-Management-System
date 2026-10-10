package com.pms.api.cases.dto;

import com.pms.api.cases.entity.Crime;
import java.time.Instant;

public record CrimeResponse(
        Long id,
        String offenceName,
        String description,
        String category,
        Instant createdAt,
        Instant updatedAt
) {
    public static CrimeResponse from(Crime crime) {
        return new CrimeResponse(
                crime.getId(),
                crime.getOffenceName(),
                crime.getDescription(),
                crime.getCategory(),
                crime.getCreatedAt(),
                crime.getUpdatedAt()
        );
    }
}
