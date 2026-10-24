package com.pms.api.health.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.health.dto.AllergyResponse;
import com.pms.api.health.dto.CreateAllergyRequest;
import com.pms.api.health.service.AllergyService;
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
@RequestMapping("/api/v1/allergies")
@Tag(name = "Allergies", description = "Inmate allergy records")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MEDICAL_OFFICER')")
public class AllergyController {

    private static final Set<String> SORT_FIELDS =
            Set.of("allergen", "severity", "recordedDate",
                    "status", "createdAt", "updatedAt");

    private final AllergyService allergyService;
    private final PaginationProperties paginationProperties;

    public AllergyController(
            AllergyService allergyService,
            PaginationProperties paginationProperties) {
        this.allergyService = allergyService;
        this.paginationProperties = paginationProperties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an inmate allergy record")
    public AllergyResponse create(
            @Valid @RequestBody CreateAllergyRequest request,
            Authentication authentication) {
        return allergyService.create(request, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List allergies with pagination and optional search")
    public PageResponse<AllergyResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {

        Pageable pageable = PageableValidator.create(
                page, size, sort, SORT_FIELDS,
                Sort.by(Sort.Direction.DESC, "createdAt"),
                paginationProperties);

        Page<AllergyResponse> result =
                allergyService.list(search, pageable);

        return PageResponse.from(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an allergy record by public UUID")
    public AllergyResponse get(@PathVariable UUID id) {
        return allergyService.get(id);
    }
}