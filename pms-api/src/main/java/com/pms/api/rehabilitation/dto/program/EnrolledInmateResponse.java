package com.pms.api.rehabilitation.dto.program;

import java.util.UUID;

public record EnrolledInmateResponse(UUID inmateId, String inmateName) {
}
