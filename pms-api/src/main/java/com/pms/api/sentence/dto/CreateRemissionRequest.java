package com.pms.api.sentence.dto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CreateRemissionRequest(@NotNull @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer=3, fraction=2) BigDecimal remissionPercentage,
        @NotNull LocalDate effectiveDate, @NotBlank String reason) { }
