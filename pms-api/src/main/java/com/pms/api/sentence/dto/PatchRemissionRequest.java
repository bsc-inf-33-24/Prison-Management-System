package com.pms.api.sentence.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PatchRemissionRequest(
        @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal remissionPercentage,
        LocalDate effectiveDate,
        @Size(min = 1) String reason) {
    @AssertTrue(message = "At least one remission field must be provided.")
    public boolean hasChanges() {
        return remissionPercentage != null || effectiveDate != null || reason != null;
    }
}
