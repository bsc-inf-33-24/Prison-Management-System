package com.pms.api.health.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.health.dto.CreateMedicalConditionRequest;
import com.pms.api.health.dto.MedicalConditionResponse;
import com.pms.api.health.service.MedicalConditionService;
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
@RequestMapping("/api/v1/medical-conditions")
@Tag(name = "Medical Conditions", description = "Inmate medical conditions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MEDICAL_OFFICER')")
public class MedicalConditionController {

    private static final Set<String> SORT_FIELDS =
            Set.of("conditionType", "conditionName", "severity",
                    "dateRecorded", "status", "createdAt", "updatedAt");

    private final MedicalConditionService conditionService;
    private final PaginationProperties paginationProperties;

    public MedicalConditionController(
            MedicalConditionService conditionService,
            PaginationProperties paginationProperties) {
        this.conditionService = conditionService;
        this.paginationProperties = paginationProperties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a medical condition")
    public MedicalConditionResponse create(
            @Valid @RequestBody CreateMedicalConditionRequest request,
            Authentication authentication) {
        return conditionService.create(request, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List medical conditions with pagination and optional search")
    public PageResponse<MedicalConditionResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {

        Pageable pageable = PageableValidator.create(
                page, size, sort, SORT_FIELDS,
                Sort.by(Sort.Direction.DESC, "createdAt"),
                paginationProperties);

        Page<MedicalConditionResponse> result =
                conditionService.list(search, pageable);

        return PageResponse.from(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a medical condition by public UUID")
    public MedicalConditionResponse get(@PathVariable UUID id) {
        return conditionService.get(id);
    }
}
