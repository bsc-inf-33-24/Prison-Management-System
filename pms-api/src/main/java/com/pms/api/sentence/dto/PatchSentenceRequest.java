package com.pms.api.sentence.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.pms.api.inmate.entity.SentenceType;
import java.time.LocalDate;

public record PatchSentenceRequest(@Pattern(regexp=".*\\S.*") @Size(max=500) String offence, SentenceType sentenceType,
        LocalDate startDate, LocalDate endDate, @Valid PatchRemissionRequest remission) {
    @AssertTrue(message = "At least one field must be provided.")
    public boolean hasChanges() {
        return offence != null || sentenceType != null || startDate != null || endDate != null || remission != null;
    }
}
