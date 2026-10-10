package com.pms.api.incident.controller;

import com.pms.api.incident.dto.IncidentHistoryItemResponse;
import com.pms.api.incident.dto.IncidentResponse;
import com.pms.api.incident.dto.LogIncidentRequest;
import com.pms.api.incident.dto.RecordOutcomeRequest;
import com.pms.api.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // Only Disciplinary Officers may log an incident (per issue #13's
    // security matrix).
    @PostMapping("/api/incidents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DISCIPLINARY_OFFICER')")
    public IncidentResponse logIncident(@Valid @RequestBody LogIncidentRequest request,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        return incidentService.logIncident(request, currentUser.getUsername());
    }

    @PatchMapping("/api/incidents/{incidentId}/outcome")
    @PreAuthorize("hasRole('DISCIPLINARY_OFFICER')")
    public IncidentResponse recordOutcome(@PathVariable Long incidentId,
                                           @Valid @RequestBody RecordOutcomeRequest request) {
        return incidentService.recordOutcome(incidentId, request);
    }

    // Both Disciplinary Officers AND Super Admins can view an inmate's
    // incident history (unlike logging/updating, which is DO-only).
    @GetMapping("/api/inmates/{inmateId}/incidents")
    @PreAuthorize("hasAnyRole('DISCIPLINARY_OFFICER', 'SUPER_ADMIN')")
    public List<IncidentHistoryItemResponse> getHistory(@PathVariable Long inmateId) {
        return incidentService.getHistory(inmateId);
    }
}