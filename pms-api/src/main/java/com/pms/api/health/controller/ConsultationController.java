package com.pms.api.health.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.health.dto.ConsultationResponse;
import com.pms.api.health.dto.CreateConsultationRequest;
import com.pms.api.health.service.ConsultationService;
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
@RequestMapping("/api/v1/consultations")
@Tag(name = "Consultations", description = "Inmate medical consultations")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MEDICAL_OFFICER')")
public class ConsultationController {

    private static final Set<String> SORT_FIELDS =
            Set.of("consultationDate", "consultedBy",
                    "createdAt", "updatedAt");

    private final ConsultationService consultationService;
    private final PaginationProperties paginationProperties;

    public ConsultationController(
            ConsultationService consultationService,
            PaginationProperties paginationProperties) {
        this.consultationService = consultationService;
        this.paginationProperties = paginationProperties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a medical consultation")
    public ConsultationResponse create(
            @Valid @RequestBody CreateConsultationRequest request,
            Authentication authentication) {
        return consultationService.create(request, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List consultations with pagination and optional search")
    public PageResponse<ConsultationResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {

        Pageable pageable = PageableValidator.create(
                page, size, sort, SORT_FIELDS,
                Sort.by(Sort.Direction.DESC, "createdAt"),
                paginationProperties);

        Page<ConsultationResponse> result =
                consultationService.list(search, pageable);

        return PageResponse.from(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a consultation by public UUID")
    public ConsultationResponse get(@PathVariable UUID id) {
        return consultationService.get(id);
    }
}