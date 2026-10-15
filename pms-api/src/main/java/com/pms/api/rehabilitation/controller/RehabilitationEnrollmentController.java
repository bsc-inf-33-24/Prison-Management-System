package com.pms.api.rehabilitation.controller;

import com.pms.api.rehabilitation.dto.enrollment.CreateEnrollmentRequest;
import com.pms.api.rehabilitation.dto.enrollment.EnrollmentResponse;
import com.pms.api.rehabilitation.dto.enrollment.UpdateEnrollmentStatusRequest;
import com.pms.api.rehabilitation.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rehabilitation/enrollments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Rehabilitation Enrollments", description = "Enroll inmates and manage rehabilitation participation")
public class RehabilitationEnrollmentController {

    private final EnrollmentService enrollmentService;

    public RehabilitationEnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('REHABILITATION_OFFICER')")
    @Operation(summary = "Enroll an inmate in a rehabilitation program")
    public EnrollmentResponse create(@Valid @RequestBody CreateEnrollmentRequest request,
                                     @AuthenticationPrincipal UserDetails currentUser) {
        return enrollmentService.create(request, currentUser.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER')")
    @Operation(summary = "List and search rehabilitation enrollments")
    public Page<EnrollmentResponse> list(
            @RequestParam(required = false) UUID inmateId,
            @RequestParam(required = false) String inmateName,
            @RequestParam(required = false) UUID programId,
            @RequestParam(required = false) String programName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate enrollmentDateFrom,
            @RequestParam(required = false) LocalDate enrollmentDateTo,
            @PageableDefault(size = 20, sort = "enrollmentDate") Pageable pageable) {
        RehabilitationPageable.validate(pageable,
                java.util.Set.of("id", "enrollmentDate", "status"));
        if (enrollmentDateFrom != null && enrollmentDateTo != null
                && enrollmentDateFrom.isAfter(enrollmentDateTo)) {
            throw new IllegalArgumentException("The enrollment date range is invalid.");
        }
        return enrollmentService.list(inmateId, inmateName, programId, programName, status,
                enrollmentDateFrom, enrollmentDateTo, pageable);
    }

    @PatchMapping("/{enrollmentId}/status")
    @PreAuthorize("hasRole('REHABILITATION_OFFICER')")
    @Operation(summary = "Update enrollment status")
    public EnrollmentResponse updateStatus(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody UpdateEnrollmentStatusRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return enrollmentService.updateStatus(enrollmentId, request, currentUser.getUsername());
    }

}
