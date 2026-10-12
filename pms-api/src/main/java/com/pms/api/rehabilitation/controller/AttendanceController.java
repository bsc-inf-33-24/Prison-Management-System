package com.pms.api.rehabilitation.controller;

import com.pms.api.rehabilitation.dto.attendance.AttendanceResponse;
import com.pms.api.rehabilitation.dto.attendance.RecordAttendanceRequest;
import com.pms.api.rehabilitation.dto.attendance.UpdateAttendanceRequest;
import com.pms.api.rehabilitation.service.AttendanceService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/rehabilitation/attendance")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Rehabilitation Attendance", description = "Record, update, search, and review attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('REHABILITATION_OFFICER')")
    @Operation(summary = "Record attendance for a rehabilitation session")
    public AttendanceResponse create(
            @Valid @RequestBody RecordAttendanceRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return attendanceService.create(request, currentUser.getUsername());
    }

    @PutMapping("/{attendanceId}")
    @PreAuthorize("hasRole('REHABILITATION_OFFICER')")
    @Operation(summary = "Update attendance")
    public AttendanceResponse update(
            @PathVariable Long attendanceId,
            @Valid @RequestBody UpdateAttendanceRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return attendanceService.update(attendanceId, request, currentUser.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER')")
    @Operation(summary = "List and search attendance records")
    public Page<AttendanceResponse> list(
            @RequestParam(required = false) Long enrollmentId,
            @RequestParam(required = false) Long inmateId,
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) String attendanceStatus,
            @RequestParam(required = false) LocalDate sessionDateFrom,
            @RequestParam(required = false) LocalDate sessionDateTo,
            @PageableDefault(size = 20, sort = "sessionDate,desc") Pageable pageable) {
        RehabilitationPageable.validate(pageable,
                java.util.Set.of("id", "sessionDate"));
        if (sessionDateFrom != null && sessionDateTo != null
                && sessionDateFrom.isAfter(sessionDateTo)) {
            throw new IllegalArgumentException("The session date range is invalid.");
        }
        return attendanceService.list(enrollmentId, inmateId, programId, attendanceStatus,
                sessionDateFrom, sessionDateTo, pageable);
    }
}
