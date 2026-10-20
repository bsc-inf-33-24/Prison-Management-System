package com.pms.api.health.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.common.pagination.PageableValidator;
import com.pms.api.common.pagination.PaginationProperties;
import com.pms.api.health.dto.CreateMedicalAppointmentRequest;
import com.pms.api.health.dto.MedicalAppointmentResponse;
import com.pms.api.health.service.MedicalAppointmentService;
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
@RequestMapping("/api/v1/appointments")
@Tag(name = "Medical Appointments", description = "Inmate medical appointments")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MEDICAL_OFFICER')")
public class MedicalAppointmentController {

    private static final Set<String> SORT_FIELDS = Set.of(
            "appointmentDate",
            "appointmentType",
            "status",
            "scheduledBy",
            "createdAt",
            "updatedAt"
    );

    private final MedicalAppointmentService appointmentService;
    private final PaginationProperties paginationProperties;

    public MedicalAppointmentController(
            MedicalAppointmentService appointmentService,
            PaginationProperties paginationProperties) {
        this.appointmentService = appointmentService;
        this.paginationProperties = paginationProperties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Schedule a medical appointment")
    public MedicalAppointmentResponse create(
            @Valid @RequestBody CreateMedicalAppointmentRequest request,
            Authentication authentication) {
        return appointmentService.create(request, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "List medical appointments with pagination and search")
    public PageResponse<MedicalAppointmentResponse> list(
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

        Page<MedicalAppointmentResponse> result =
                appointmentService.list(search, pageable);

        return PageResponse.from(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a medical appointment by public ID")
    public MedicalAppointmentResponse get(@PathVariable UUID id) {
        return appointmentService.get(id);
    }
}