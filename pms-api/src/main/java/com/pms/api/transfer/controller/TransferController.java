package com.pms.api.transfer.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.transfer.dto.ChangeTransferStatusRequest;
import com.pms.api.transfer.dto.CreateTransferRequest;
import com.pms.api.transfer.dto.TransferResponse;
import com.pms.api.transfer.entity.TransferStatus;
import com.pms.api.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/transfers") @SecurityRequirement(name="bearerAuth")
@Tag(name="06. Inmate transfers",description="Transfer inmates between prisons in the centralized PMS.")
public class TransferController {
    private final TransferService service;
    public TransferController(TransferService service) { this.service=service; }

    @PreAuthorize("hasAnyRole('RECORDS_OFFICER','COMMISSIONER','TRANSFER_OFFICER','DISCIPLINARY_OFFICER','CASE_OFFICER','VISITOR_OFFICER','INSTRUCTOR','MEDICAL_OFFICER','DUTY_OFFICER','PROGRAM_FACILITATOR')")
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request an inmate transfer",
            description = "Creates a pending transfer request for an inmate. The destination prison is optional and may be supplied during approval. TRANSFER_OFFICER can request transfers system-wide; other prison-scoped roles are restricted to their assigned prison.")
    public TransferResponse create(@Valid @RequestBody CreateTransferRequest request) { return service.create(request); }
    @GetMapping
    @Operation(summary = "List inmate transfers",
            description = "Returns transfers visible to Records Officers, Commissioners, and Transfer Officers. Transfer Officers can view transfers across all prisons. Optional filters are transfer status and prison UUID; results can be paged and sorted.")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER','COMMISSIONER','TRANSFER_OFFICER')")
    public PageResponse<TransferResponse> list(@RequestParam(required=false) TransferStatus status,
            @RequestParam(required=false) UUID prisonId,@RequestParam(required=false) Integer page,
            @RequestParam(required=false) Integer size,@RequestParam(required=false) List<String> sort) {
        return service.list(status,prisonId,page,size,sort);
    }
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER','COMMISSIONER','TRANSFER_OFFICER')")
    @Operation(summary = "Change a transfer status",
            description = "Approves or rejects transfers as a Commissioner or Transfer Officer. Approval may include destinationPrisonId when the request has no destination; it is optional when one is already recorded. Records Officers may confirm receipt or cancel transfers at their assigned prison.")
    public TransferResponse changeStatus(@PathVariable UUID id,@Valid @RequestBody ChangeTransferStatusRequest request) {
        return service.changeStatus(id,request);
    }
}
