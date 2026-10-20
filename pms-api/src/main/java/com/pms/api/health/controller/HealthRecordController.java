package com.pms.api.health.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.health.dto.CreateHealthRecordRequest;
import com.pms.api.health.dto.HealthRecordResponse;
import com.pms.api.health.service.HealthRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/health-records")
@Tag(name = "Health Records", description = "Inmate health screening records")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MEDICAL_OFFICER')")
public class HealthRecordController {

    private static final Set<String> SORT_FIELDS =
            Set.of("screeningDate", "status", "createdAt", "updatedAt");

    private final HealthRecordService healthRecordService;
    private final PaginationProperties paginationProperties;

    public HealthRecordController(HealthRecordService healthRecordService,
                                  PaginationProperties paginationProperties) {
        this.healthRecordService = healthRecordService;
        this.paginationProperties = paginationProperties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an inmate health record")
    public HealthRecordResponse create(@Valid @RequestBody CreateHealthRecordRequest request,
                                       Authentication authentication) {
        return healthRecordService.create(request, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List health records with pagination and optional search")
    public PageResponse<HealthRecordResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {

        Pageable pageable = PageableValidator.create(
                page, size, sort, SORT_FIELDS,
                Sort.by(Sort.Direction.DESC, "createdAt"), paginationProperties);

        Page<HealthRecordResponse> result = healthRecordService.list(search, pageable);
        return PageResponse.from(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a health record by public UUID")
    public HealthRecordResponse get(@PathVariable UUID id) {
        return healthRecordService.get(id);
    }
}
