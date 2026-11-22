package com.pms.api.prison.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.prison.dto.CreateBlockRequest;
import com.pms.api.prison.dto.CreatePrisonRequest;
import com.pms.api.prison.dto.PrisonDetailResponse;
import com.pms.api.prison.dto.PrisonListResponse;
import com.pms.api.prison.dto.PrisonResponse;
import com.pms.api.prison.dto.UpdatePrisonRequest;
import com.pms.api.prison.dto.UpdateBlockRequest;
import com.pms.api.prison.service.PrisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/prisons")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "02. Prison and facility setup", description = "Create prisons with their initial blocks and cells, then manage the facility structure.")
public class PrisonController {
    private final PrisonService service;
    public PrisonController(PrisonService service) { this.service=service; }

    @PostMapping @PreAuthorize("hasRole('SUPER_ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a prison with its initial blocks and cells", description = "Creates a prison and its supplied initial facility structure in one request. Requires SUPER_ADMIN.")
    public PrisonDetailResponse create(@Valid @RequestBody CreatePrisonRequest request) { return service.create(request); }

    @GetMapping @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN', 'COMMISSIONER', 'TRANSFER_OFFICER')")
    @Operation(summary = "List prisons", description = "Searches prisons by name or code, optionally filters by active status, and returns a paged result. TRANSFER_OFFICER can list all prisons.")
    public PageResponse<PrisonListResponse> list(@RequestParam(required=false) String search,
            @RequestParam(required=false) Boolean active, @RequestParam(required=false) Integer page,
            @RequestParam(required=false) Integer size, @RequestParam(required=false) List<String> sort) {
        return service.list(search,active,page,size,sort);
    }

    @GetMapping("/{prisonId}") @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN', 'COMMISSIONER', 'TRANSFER_OFFICER')")
    @Operation(summary = "Get prison details", description = "Returns a prison and its facility structure by public UUID. TRANSFER_OFFICER can view any prison.")
    public PrisonDetailResponse get(@PathVariable UUID prisonId) { return service.get(prisonId); }

    @PatchMapping("/{prisonId}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update prison details", description = "Updates the supplied prison details. Requires SUPER_ADMIN.")
    public PrisonResponse update(@PathVariable UUID prisonId,@Valid @RequestBody UpdatePrisonRequest request) { return service.update(prisonId,request); }

    @PostMapping("/{prisonId}/blocks") @PreAuthorize("hasRole('SUPER_ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a block and its cells to a prison", description = "Creates a block and the supplied cells under the prison UUID. Requires SUPER_ADMIN.")
    public PrisonDetailResponse addBlock(@PathVariable UUID prisonId,
                                         @Valid @RequestBody CreateBlockRequest request) {
        return service.addBlock(prisonId,request);
    }

    @PatchMapping("/{prisonId}/blocks/{blockId}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update a block and its cells", description = "Renames the specified block and/or adds cells according to the supplied fields. Both prisonId and blockId are public UUIDs. Requires SUPER_ADMIN.")
    public PrisonDetailResponse updateBlock(@PathVariable UUID prisonId, @PathVariable UUID blockId,
                                            @Valid @RequestBody UpdateBlockRequest request) {
        return service.updateBlock(prisonId,blockId,request);
    }

}
