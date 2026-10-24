package com.pms.api.health.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.health.dto.CreateMedicationRequest;
import com.pms.api.health.dto.MedicationResponse;
import com.pms.api.health.service.MedicationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medications")
@Tag(name = "Medications", description = "Inmate medication records")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MEDICAL_OFFICER')")
public class MedicationController {

    private static final Set<String> SORT_FIELDS = Set.of(
            "startDate",
            "endDate",
            "status",
            "prescribedBy",
            "createdAt",
            "updatedAt"
    );

    private final MedicationService medicationService;
    private final PaginationProperties paginationProperties;

    public MedicationController(
            MedicationService medicationService,
            PaginationProperties paginationProperties) {
        this.medicationService = medicationService;
        this.paginationProperties = paginationProperties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a medication record")
    public MedicationResponse create(
            @Valid @RequestBody CreateMedicationRequest request,
            Authentication authentication) {
        return medicationService.create(request, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List medication records with pagination and search")
    public PageResponse<MedicationResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {

        Pageable pageable = PageableValidator.create(
                page,
                size,
                sort,
                SORT_FIELDS,
                Sort.by(Sort.Direction.DESC, "createdAt"),
                paginationProperties
        );

        Page<MedicationResponse> result =
                medicationService.list(search, pageable);

        return PageResponse.from(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a medication record by public ID")
    public MedicationResponse get(@PathVariable UUID id) {
        return medicationService.get(id);
    }
}