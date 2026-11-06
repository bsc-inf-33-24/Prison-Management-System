package com.pms.api.prison.dto;

import com.pms.api.prison.entity.Prison;
import java.util.UUID;

public record PrisonListResponse(UUID id, String name, String location, boolean active) {
    public static PrisonListResponse from(Prison prison) {
        return new PrisonListResponse(
                prison.getPublicUuid(),
                prison.getName(),
                prison.getLocation(),
                prison.isActive());
    }
}
