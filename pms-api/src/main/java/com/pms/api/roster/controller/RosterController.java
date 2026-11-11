package com.pms.api.roster.controller;

import com.pms.api.roster.dto.CreateRosterRequest;
import com.pms.api.roster.dto.CreateShiftRequest;
import com.pms.api.roster.dto.RosterResponse;
import com.pms.api.roster.dto.RosterShiftResponse;
import com.pms.api.roster.dto.RosterSummaryResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a draft duty roster")
    public RosterResponse create(@Valid @RequestBody CreateRosterRequest request) {
        return rosterService.create(request);
    }

    @GetMapping("/rosters")
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List rosters visible to the caller")
    public List<RosterSummaryResponse> list(
            @RequestParam(required = false) UUID prisonId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) RosterStatus status) {
        return rosterService.list(prisonId, from, to, status);
    }

    @GetMapping("/rosters/{rosterId}")
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "View a roster and its shifts")
    public RosterResponse get(@PathVariable UUID rosterId) {
        return rosterService.get(rosterId);
    }

    @PatchMapping("/rosters/{rosterId}")
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Change a draft roster's date range")
    public RosterResponse update(@PathVariable UUID rosterId,
                                 @Valid @RequestBody UpdateRosterRequest request) {
        return rosterService.update(rosterId, request);
    }

    @PostMapping("/rosters/{rosterId}/shifts")
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a shift to a draft roster")
    public RosterShiftResponse addShift(@PathVariable UUID rosterId,
                                        @Valid @RequestBody CreateShiftRequest request) {
        return rosterService.addShift(rosterId, request);
    }

    @PatchMapping("/rosters/{rosterId}/shifts/{shiftId}")
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Update a shift")
    public RosterShiftResponse updateShift(@PathVariable UUID rosterId,
                                           @PathVariable UUID shiftId,
                                           @Valid @RequestBody UpdateShiftRequest request) {
        return rosterService.updateShift(rosterId, shiftId, request);
    }

    @DeleteMapping("/rosters/{rosterId}/shifts/{shiftId}")
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a draft shift or cancel a published shift")
    public void deleteShift(@PathVariable UUID rosterId, @PathVariable UUID shiftId) {
        rosterService.deleteShift(rosterId, shiftId);
    }

    @PostMapping("/rosters/{rosterId}/publish")
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Validate and publish a roster")
    public RosterResponse publish(@PathVariable UUID rosterId) {
        return rosterService.publish(rosterId);
    }

    @PostMapping("/rosters/{rosterId}/close")
    @PreAuthorize("hasAnyRole('DUTY_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Close a published roster after its period ends")
    public RosterResponse close(@PathVariable UUID rosterId) {
        return rosterService.close(rosterId);
    }

    @GetMapping("/me/shifts")
    @PreAuthorize("hasRole('PRISON_OFFICER')")
    @Operation(summary = "View the authenticated officer's published shifts")
    public List<RosterShiftResponse> myShifts(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return rosterService.myShifts(from, to);
    }
}
