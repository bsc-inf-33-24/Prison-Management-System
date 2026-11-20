package com.pms.api.sentence.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

public record PatchSentenceRequest(@Valid PatchRemissionRequest remission) {
    @AssertTrue(message = "Remission details must be provided.")
    public boolean hasChanges() { return remission != null; }
}
