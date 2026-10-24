package com.pms.api.rehabilitation.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RecordAttendanceEntryRequest(
        @NotNull UUID inmateId,
        @NotBlank String attendanceStatus,
        @Size(max = 10000) String remarks
) {
}
