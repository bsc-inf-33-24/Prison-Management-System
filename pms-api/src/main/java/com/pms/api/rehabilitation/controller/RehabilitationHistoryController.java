package com.pms.api.rehabilitation.controller;

import com.pms.api.rehabilitation.dto.attendance.AttendanceResponse;
import com.pms.api.rehabilitation.dto.enrollment.EnrollmentResponse;
import com.pms.api.rehabilitation.service.AttendanceService;
import com.pms.api.rehabilitation.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Rehabilitation History", description = "View inmate participation and enrollment attendance history")
public class RehabilitationHistoryController {

    private final EnrollmentService enrollmentService;
    private final AttendanceService attendanceService;

    public RehabilitationHistoryController(EnrollmentService enrollmentService,
                                            AttendanceService attendanceService) {
        this.enrollmentService = enrollmentService;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/inmates/{inmateId}/rehabilitation-history")
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER')")
    @Operation(summary = "Get an inmate's rehabilitation history")
    public Page<EnrollmentResponse> inmateHistory(
            @PathVariable Long inmateId,
            @PageableDefault(size = 20, sort = "enrollmentDate") Pageable pageable) {
        RehabilitationPageable.validate(pageable,
                java.util.Set.of("id", "enrollmentDate", "status"));
        return enrollmentService.history(inmateId, pageable);
    }

    @GetMapping("/rehabilitation/enrollments/{enrollmentId}/attendance")
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER')")
    @Operation(summary = "Get attendance history for an enrollment")
    public Page<AttendanceResponse> enrollmentAttendance(
            @PathVariable Long enrollmentId,
            @PageableDefault(size = 20, sort = "sessionDate,desc") Pageable pageable) {
        RehabilitationPageable.validate(pageable,
                java.util.Set.of("id", "sessionDate"));
        return attendanceService.history(enrollmentId, pageable);
    }

}
