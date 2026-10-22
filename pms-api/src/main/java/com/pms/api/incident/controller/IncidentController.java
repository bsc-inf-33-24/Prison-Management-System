package com.pms.api.incident.controller;

import com.pms.api.incident.dto.IncidentHistoryItemResponse;
import com.pms.api.incident.dto.IncidentResponse;
import com.pms.api.incident.dto.LogIncidentRequest;
import com.pms.api.incident.dto.RecordOutcomeRequest;
import com.pms.api.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "5. Incidents and discipline", description = "Log incidents, record outcomes, and review inmate discipline history.")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // Only Disciplinary Officers may log an incident (per issue #13's
    // security matrix).
    @PostMapping("/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DISCIPLINARY_OFFICER')")
    @Operation(summary = "Log an inmate incident")
    public IncidentResponse logIncident(@Valid @RequestBody LogIncidentRequest request,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        return incidentService.logIncident(request, currentUser.getUsername());
    }

    @PatchMapping("/incidents/{incidentId}/outcome")
    @PreAuthorize("hasRole('DISCIPLINARY_OFFICER')")
    @Operation(summary = "Record a disciplinary outcome")
    public IncidentResponse recordOutcome(@PathVariable UUID incidentId,
                                           @Valid @RequestBody RecordOutcomeRequest request) {
        return incidentService.recordOutcome(incidentId, request);
    }

    // Both Disciplinary Officers AND Super Admins can view an inmate's
    // incident history (unlike logging/updating, which is DO-only).
    @GetMapping("/inmates/{inmateId}/incidents")
    @PreAuthorize("hasAnyRole('DISCIPLINARY_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Get an inmate's incident history")
    public List<IncidentHistoryItemResponse> getHistory(@PathVariable UUID inmateId) {
        return incidentService.getHistory(inmateId);
    }
}
