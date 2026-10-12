package com.pms.api.rehabilitation.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAttendanceRequest(
        @NotBlank String attendanceStatus,
        @Size(max = 10000) String remarks
) {
}
