package com.pms.api.visitor.controller;

import com.pms.api.visitor.dto.VisitRequest;
import com.pms.api.visitor.dto.VisitResponse;
import com.pms.api.visitor.service.VisitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "09. Visits Management", description = "Manage family, legal, and medical visits.")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "Record a visit", description = "Record a family, legal, or health visit using the type field. Type-specific details are supplied in the same request.")
    @PostMapping("/visits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'CASE_OFFICER', 'RECORDS_OFFICER', 'MEDICAL_OFFICER')")
    public VisitResponse recordVisit(@Valid @RequestBody VisitRequest request,
                                    @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.recordVisit(request, currentUser.getUsername());
    }

    @Operation(summary = "Search visits", description = "Filters visits by visit UUID, visitor name, visit type, inmate UUID or name, visit date range, and recording officer UUID. Results are restricted by the authenticated user's role.")
    @GetMapping("/visits")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'CASE_OFFICER', 'RECORDS_OFFICER', 'MEDICAL_OFFICER')")
    public Page<VisitResponse> searchVisits(
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) String visitorName,
            @RequestParam(required = false) String visitType,
            @RequestParam(required = false) UUID inmateId,
            @RequestParam(required = false) String inmateName,
            @RequestParam(required = false) OffsetDateTime visitDateFrom,
            @RequestParam(required = false) OffsetDateTime visitDateTo,
            @RequestParam(required = false) UUID loggedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.searchVisits(visitId, visitorName, visitType, inmateId,
                inmateName, visitDateFrom, visitDateTo, loggedBy, page, size, sort, currentUser.getUsername());
    }

    @Operation(summary = "Get visits for an inmate", description = "Returns visits for the inmate UUID, filtered by the authenticated user's role: Medical Officers see health visits; other roles see their authorized family and legal visits.")
    @GetMapping("/visits/inmate/{inmateId}")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'CASE_OFFICER', 'RECORDS_OFFICER', 'MEDICAL_OFFICER')")
    public Page<VisitResponse> getVisitsByInmate(
            @PathVariable UUID inmateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.searchVisits(null, null, null, inmateId, null,
                null, null, null, page, size, sort, currentUser.getUsername());
    }

}
