package com.pms.api.roster.controller;

import com.pms.api.roster.dto.CreateRosterRequest;
import com.pms.api.roster.dto.RosterResponse;
import com.pms.api.roster.dto.RosterShiftResponse;
import com.pms.api.roster.dto.UpdateRosterRequest;
import com.pms.api.roster.dto.UpdateShiftRequest;
import com.pms.api.roster.entity.RosterStatus;
import com.pms.api.roster.service.RosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@Tag(name = "10. Duty rosters", description = "Create, validate, publish, and close prison duty rosters.")
public class RosterController {

    private final RosterService rosterService;

    public RosterController(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    @PostMapping("/rosters")
    @PreAuthorize("hasRole('DUTY_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a duty roster", description = "Creates a draft roster and assigns all shifts in the request. Requires DUTY_OFFICER.")
    public RosterResponse create(@Valid @RequestBody CreateRosterRequest request) {
        return rosterService.create(request);
    }

    @GetMapping("/rosters")
    @PreAuthorize("hasRole('DUTY_OFFICER')")
    @Operation(summary = "List rosters visible to the caller", description = "Lists rosters, optionally filtered by prison UUID, inclusive date range, and status. Requires DUTY_OFFICER.")
    public List<RosterResponse> list(
            @RequestParam(required = false) UUID rosterId,
            @RequestParam(required = false) UUID prisonId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) RosterStatus status) {
        return rosterService.list(rosterId, prisonId, from, to, status);
    }

    @PatchMapping("/rosters/{rosterId}")
    @PreAuthorize("hasRole('DUTY_OFFICER')")
    @Operation(summary = "Update a duty roster", description = "Updates the date range, adds or updates shifts, removes shifts by ID, or changes status to PUBLISHED or CLOSED. Requires DUTY_OFFICER.")
    public RosterResponse update(@PathVariable UUID rosterId,
                                 @Valid @RequestBody UpdateRosterRequest request) {
        return rosterService.update(rosterId, request);
    }

    @GetMapping("/me/shifts")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @Operation(summary = "View the authenticated officer's published shifts", description = "Returns the authenticated Records Officer's published shifts within the required inclusive from/to dates. Dates use YYYY-MM-DD.")
    public List<RosterShiftResponse> myShifts(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return rosterService.myShifts(from, to);
    }
}
