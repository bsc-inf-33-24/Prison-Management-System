package com.pms.api.transfer.dto;

import jakarta.validation.constraints.Size;

public record TransferDecisionRequest(@Size(max=2000) String note) { }
