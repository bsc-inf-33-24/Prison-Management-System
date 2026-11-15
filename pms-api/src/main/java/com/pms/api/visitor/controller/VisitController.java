package com.pms.api.visitor.controller;

import com.pms.api.visitor.dto.VisitHistoryResponse;
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
@Tag(name = "09. Visits Management")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "Record a visit", description = "Record a family, legal, or health visit. Health details are restricted to Medical Officers.")
    @PostMapping("/visits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER', 'CASE_OFFICER', 'MEDICAL_OFFICER', 'SUPER_ADMIN')")
    public VisitResponse recordVisit(@Valid @RequestBody VisitRequest request,
                                    @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.recordVisit(request, currentUser.getUsername());
    }

    @Operation(summary = "Search visits", description = "Filter visits by type, recorded visitor details, inmate, date range, and recording officer.")
    @GetMapping("/visits")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER', 'CASE_OFFICER', 'MEDICAL_OFFICER', 'SUPER_ADMIN')")
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

    @Operation(summary = "Get an inmate's visit history", description = "Returns visits associated with the specified inmate, including each visitor's name and visit type.")
    @GetMapping("/inmates/{inmateId}/visits")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER', 'CASE_OFFICER', 'MEDICAL_OFFICER', 'SUPER_ADMIN')")
    public Page<VisitHistoryResponse> getInmateVisitHistory(
            @PathVariable UUID inmateId,
            @RequestParam(required = false) String visitorName,
            @RequestParam(required = false) String visitType,
            @RequestParam(required = false) OffsetDateTime visitDateFrom,
            @RequestParam(required = false) OffsetDateTime visitDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.getInmateVisitHistory(inmateId, visitorName, visitType, visitDateFrom, visitDateTo,
                page, size, sort, currentUser.getUsername());
    }

}
