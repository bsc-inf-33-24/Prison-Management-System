package com.pms.api.rehabilitation.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RecordAttendanceRequest(
        @NotNull Long enrollmentId,
        @NotNull LocalDate sessionDate,
        @NotBlank String attendanceStatus,
        @Size(max = 10000) String remarks
) {
}
