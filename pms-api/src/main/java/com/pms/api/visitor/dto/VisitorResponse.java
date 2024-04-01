package com.pms.api.visitor.dto;

import com.pms.api.visitor.entity.Visitor;
import java.time.Instant;

public record VisitorResponse(
        Long id,
        String fullName,
        String idNumber,
        String phone,
        String relationshipToInmate,
        Instant createdAt,
        Instant updatedAt
) {
    public static VisitorResponse from(Visitor visitor) {
        return new VisitorResponse(
                visitor.getId(),
                visitor.getFullName(),
                visitor.getIdNumber(),
                visitor.getPhone(),
                visitor.getRelationshipToInmate(),
                visitor.getCreatedAt(),
                visitor.getUpdatedAt()
        );
    }
}
