package com.pms.api.rehabilitation.dto.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BulkRecordAttendanceRequest(
        @NotNull UUID programId,
        @NotNull LocalDate sessionDate,
        @NotEmpty @Size(max = 500) List<@NotNull @Valid RecordAttendanceEntryRequest> records
) {
}
