package com.pms.api.rehabilitation.controller;

import com.pms.api.rehabilitation.dto.attendance.AttendanceResponse;
import com.pms.api.rehabilitation.dto.attendance.BulkRecordAttendanceRequest;
import com.pms.api.rehabilitation.dto.attendance.RecordAttendanceRequest;
import com.pms.api.rehabilitation.dto.enrollment.CreateEnrollmentRequest;
import com.pms.api.rehabilitation.dto.enrollment.EnrollmentResponse;
import com.pms.api.rehabilitation.dto.program.CreateProgramRequest;
import com.pms.api.rehabilitation.dto.program.ProgramDetailResponse;
import com.pms.api.rehabilitation.dto.program.ProgramResponse;
import com.pms.api.rehabilitation.dto.program.UpdateProgramRequest;
import com.pms.api.rehabilitation.service.AttendanceService;
import com.pms.api.rehabilitation.service.EnrollmentService;
import com.pms.api.rehabilitation.service.RehabilitationProgramService;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Set;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "10. Rehabilitation", description = "Manage rehabilitation programs, enrollments, attendance, and inmate history")
public class RehabilitationController {

    private final RehabilitationProgramService programService;
    private final EnrollmentService enrollmentService;
    private final AttendanceService attendanceService;

    public RehabilitationController(RehabilitationProgramService programService,
                                    EnrollmentService enrollmentService,
                                    AttendanceService attendanceService) {
        this.programService = programService;
        this.enrollmentService = enrollmentService;
        this.attendanceService = attendanceService;
    }

    @PostMapping("/rehabilitation/programs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Create a rehabilitation program")
    public ProgramResponse createProgram(@Valid @RequestBody CreateProgramRequest request,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        return programService.create(request, currentUser.getUsername());
    }

    @GetMapping("/rehabilitation/programs")
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List and search rehabilitation programs",
            description = "Each program includes the inmates currently enrolled in it.")
    public Page<ProgramResponse> listPrograms(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String schedule,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        RehabilitationPageable.validate(pageable, Set.of("id", "name", "instructor", "status", "schedule"));
        return programService.list(name, instructor, status, schedule, pageable);
    }

    @GetMapping("/rehabilitation/programs/{programId}")
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Get a rehabilitation program")
    public ProgramDetailResponse getProgram(@PathVariable UUID programId) {
        return programService.getDetails(programId);
    }

    @PatchMapping("/rehabilitation/programs/{programId}")
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Update rehabilitation program details")
    public ProgramResponse updateProgram(@PathVariable UUID programId,
                                         @Valid @RequestBody UpdateProgramRequest request,
                                         @AuthenticationPrincipal UserDetails currentUser) {
        return programService.update(programId, request, currentUser.getUsername());
    }

    @PostMapping("/rehabilitation/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Enroll an inmate in a rehabilitation program")
    public EnrollmentResponse createEnrollment(@Valid @RequestBody CreateEnrollmentRequest request,
                                                @AuthenticationPrincipal UserDetails currentUser) {
        return enrollmentService.create(request, currentUser.getUsername());
    }

    @GetMapping("/rehabilitation/enrollments")
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List and search rehabilitation enrollments")
    public Page<EnrollmentResponse> listEnrollments(
            @RequestParam(required = false) UUID inmateId,
            @RequestParam(required = false) String inmateName,
            @RequestParam(required = false) UUID programId,
            @RequestParam(required = false) String programName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        return enrollmentService.list(inmateId, inmateName, programId, programName, status,
                PageRequest.of(page, size, Sort.by("enrollmentDate")));
    }

    @PostMapping("/rehabilitation/attendance")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Record attendance for a rehabilitation session")
    public AttendanceResponse createAttendance(@Valid @RequestBody RecordAttendanceRequest request,
                                               @AuthenticationPrincipal UserDetails currentUser) {
        return attendanceService.create(request, currentUser.getUsername());
    }

    @PostMapping("/rehabilitation/attendance/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Record attendance for multiple inmates in one session")
    public List<AttendanceResponse> createBulkAttendance(
            @Valid @RequestBody BulkRecordAttendanceRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return attendanceService.createBulk(request, currentUser.getUsername());
    }

    @GetMapping("/rehabilitation/attendance")
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List and search attendance records",
            description = "Combine optional filters to find attendance by inmate, program name, status, or session date range. "
                    + "Date bounds are inclusive and use YYYY-MM-DD. Results default to newest session first; page size is limited to 100.")
    public Page<AttendanceResponse> listAttendance(
            @Parameter(description = "Inmate public UUID.")
            @RequestParam(required = false) UUID inmateId,
            @Parameter(description = "Case-insensitive partial program name.")
            @RequestParam(required = false) String programName,
            @Parameter(description = "Attendance result: present or absent.")
            @RequestParam(required = false) String attendanceStatus,
            @Parameter(description = "Include sessions on or after this date (YYYY-MM-DD).")
            @RequestParam(required = false) LocalDate sessionDateFrom,
            @Parameter(description = "Include sessions on or before this date (YYYY-MM-DD).")
            @RequestParam(required = false) LocalDate sessionDateTo,
            @ParameterObject @PageableDefault(size = 20, sort = "sessionDate,desc") Pageable pageable) {
        RehabilitationPageable.validate(pageable, Set.of("id", "sessionDate"));
        if (sessionDateFrom != null && sessionDateTo != null && sessionDateFrom.isAfter(sessionDateTo)) {
            throw new IllegalArgumentException("The session date range is invalid.");
        }
        return attendanceService.list(inmateId, programName, attendanceStatus,
                sessionDateFrom, sessionDateTo, pageable);
    }

}
