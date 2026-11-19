package com.pms.api.transfer.dto;

import jakarta.validation.constraints.Size;

public record ApproveTransferRequest(@Size(max = 2000) String note) { }
