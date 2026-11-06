package com.pms.api.prison.dto;

import com.pms.api.prison.entity.Prison;
import java.util.UUID;

public record PrisonResponse(UUID id, String code, String name, String location, boolean active) {
    public static PrisonResponse from(Prison p) { return new PrisonResponse(p.getPublicUuid(), p.getCode(), p.getName(), p.getLocation(), p.isActive()); }
}
