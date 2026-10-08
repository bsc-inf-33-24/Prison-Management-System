package com.pms.api.user.dto;

import com.pms.api.user.entity.User;
import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
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
                user.getId(), user.getFullName(), user.getUsername(), user.getRole(),
                user.getStatus(), user.getPermissions(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
