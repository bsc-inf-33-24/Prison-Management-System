package com.pms.api.inmate.dto;

import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record PatchRemandRequest(LocalDate expectedCourtDate) {
    @AssertTrue(message = "An expectedCourtDate must be provided.")
    public boolean hasExpectedCourtDate() { return expectedCourtDate != null; }
}
