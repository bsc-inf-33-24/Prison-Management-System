package com.pms.api.transfer.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.transfer.dto.ApproveTransferRequest;
import com.pms.api.transfer.dto.CreateTransferRequest;
import com.pms.api.transfer.dto.TransferDecisionRequest;
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

    @PreAuthorize("hasAnyRole('RECORDS_OFFICER','PRISON_OFFICER')")
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse create(@Valid @RequestBody CreateTransferRequest request) { return service.create(request); }
    @GetMapping
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER','PRISON_OFFICER')")
    public PageResponse<TransferResponse> list(@RequestParam(required=false) TransferStatus status,
            @RequestParam(required=false) UUID prisonId,@RequestParam(required=false) Integer page,
            @RequestParam(required=false) Integer size,@RequestParam(required=false) List<String> sort) {
        return service.list(status,prisonId,page,size,sort);
    }
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve and complete an inmate transfer")
    public TransferResponse approve(@PathVariable UUID id,@Valid @RequestBody ApproveTransferRequest request) {
        return service.approve(id,request);
    }
    @PostMapping("/{id}/reject") @PreAuthorize("false") public TransferResponse reject(@PathVariable UUID id,@RequestBody(required=false) TransferDecisionRequest request) { return service.reject(id,request); }
    @PostMapping("/{id}/cancel") @PreAuthorize("false") public TransferResponse cancel(@PathVariable UUID id,@RequestBody(required=false) TransferDecisionRequest request) { return service.cancel(id,request); }
}
