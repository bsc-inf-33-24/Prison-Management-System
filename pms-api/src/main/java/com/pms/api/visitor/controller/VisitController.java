package com.pms.api.visitor.controller;

import com.pms.api.visitor.dto.UpdateVisitRequest;
import com.pms.api.visitor.dto.VisitHistoryResponse;
import com.pms.api.visitor.dto.VisitRequest;
import com.pms.api.visitor.dto.VisitResponse;
import com.pms.api.visitor.service.VisitService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "7. Visits Management")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "Record a visit", description = "Record visitor name, inmate, visit date, and start/end times; duration is calculated.")
    @PostMapping("/visits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'SUPER_ADMIN', 'RECORDS_OFFICER')")
    public VisitResponse recordVisit(@Valid @RequestBody VisitRequest request,
                                    @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.recordVisit(request, currentUser.getUsername());
    }

    @Operation(summary = "Get a visit by ID", description = "Find a specific visit with its recorded visitor details.")
    @GetMapping("/visits/{visitId}")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER')")
    public VisitResponse getVisit(@PathVariable UUID visitId) {
        return visitService.getVisit(visitId);
    }

    @Operation(summary = "Search visits", description = "Filter visits by recorded visitor details, inmate, date range, and recording officer.")
    @GetMapping("/visits")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'SUPER_ADMIN', 'RECORDS_OFFICER')")
    public Page<VisitResponse> searchVisits(
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) String visitorName,
            @RequestParam(required = false) UUID inmateId,
            @RequestParam(required = false) String inmateNumber,
            @RequestParam(required = false) String inmateName,
            @RequestParam(required = false) OffsetDateTime visitDateFrom,
            @RequestParam(required = false) OffsetDateTime visitDateTo,
            @RequestParam(required = false) UUID loggedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return visitService.searchVisits(visitId, visitorName, inmateId, inmateNumber,
                inmateName, visitDateFrom, visitDateTo, loggedBy, page, size, sort);
    }

    @Operation(summary = "Get an inmate's visit history", description = "Returns visits associated with the specified inmate, including each visitor's name.")
    @GetMapping("/inmates/{inmateId}/visits")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'SUPER_ADMIN', 'RECORDS_OFFICER')")
    public Page<VisitHistoryResponse> getInmateVisitHistory(
            @PathVariable UUID inmateId,
            @RequestParam(required = false) String visitorName,
            @RequestParam(required = false) OffsetDateTime visitDateFrom,
            @RequestParam(required = false) OffsetDateTime visitDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return visitService.getInmateVisitHistory(inmateId, visitorName, visitDateFrom, visitDateTo, page, size, sort);
    }

    @Operation(summary = "Update a visit", description = "Correct the visit start and end timestamps; duration is recalculated.")
    @PatchMapping("/visits/{visitId}")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER','SUPER_ADMIN')")
    public VisitResponse updateVisit(@PathVariable UUID visitId,
                                    @Valid @RequestBody UpdateVisitRequest request,
                                    @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.updateVisit(visitId, request, currentUser.getUsername());
    }
}
