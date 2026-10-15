package com.pms.api.user.dto;

import com.pms.api.user.entity.User;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String username,
        String role,
        String status,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getPublicUuid(), user.getFullName(), user.getUsername(), user.getRole(),
                user.getStatus(), new LinkedHashSet<> (user.getPermissions()), user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
