package com.pms.api.visitor.dto;

import com.pms.api.visitor.entity.Visitor;
import java.time.Instant;
import java.util.UUID;

public record VisitorResponse(
        UUID id,
        String fullName,
        String idNumber,
        String phone,
        String relationshipToInmate,
        Instant createdAt,
        Instant updatedAt
) {
    public static VisitorResponse from(Visitor visitor) {
        return new VisitorResponse(
                visitor.getPublicUuid(),
                visitor.getFullName(),
                visitor.getIdNumber(),
                visitor.getPhone(),
                visitor.getRelationshipToInmate(),
                visitor.getCreatedAt(),
                visitor.getUpdatedAt()
        );
    }
}
