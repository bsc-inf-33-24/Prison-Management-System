package com.pms.api.rehabilitation.controller;

import com.pms.api.rehabilitation.dto.program.CreateProgramRequest;
import com.pms.api.rehabilitation.dto.program.ProgramResponse;
import com.pms.api.rehabilitation.dto.program.ProgramDetailResponse;
import com.pms.api.rehabilitation.dto.program.UpdateProgramRequest;
import com.pms.api.rehabilitation.dto.program.UpdateProgramStatusRequest;
import com.pms.api.rehabilitation.service.RehabilitationProgramService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rehabilitation/programs")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Rehabilitation Programs", description = "Create, search, update, and manage rehabilitation programs")
public class RehabilitationProgramController {

    private final RehabilitationProgramService programService;

    public RehabilitationProgramController(RehabilitationProgramService programService) {
        this.programService = programService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('REHABILITATION_OFFICER')")
    @Operation(summary = "Create a rehabilitation program")
    public ProgramResponse create(@Valid @RequestBody CreateProgramRequest request,
                                  @AuthenticationPrincipal UserDetails currentUser) {
        return programService.create(request, currentUser.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER')")
    @Operation(summary = "List and search rehabilitation programs")
    public Page<ProgramResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String instructor,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String schedule,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        RehabilitationPageable.validate(pageable,
                java.util.Set.of("id", "name", "instructor", "status", "schedule"));
        return programService.list(name, instructor, status, schedule, pageable);
    }

    @GetMapping("/{programId}")
    @PreAuthorize("hasAnyRole('REHABILITATION_OFFICER', 'RECORDS_OFFICER')")
    @Operation(summary = "Get a rehabilitation program")
    public ProgramDetailResponse get(@PathVariable UUID programId) {
        return programService.getDetails(programId);
    }

    @PatchMapping("/{programId}")
    @PreAuthorize("hasRole('REHABILITATION_OFFICER')")
    @Operation(summary = "Update rehabilitation program details")
    public ProgramResponse update(@PathVariable UUID programId,
                                  @Valid @RequestBody UpdateProgramRequest request,
                                  @AuthenticationPrincipal UserDetails currentUser) {
        return programService.update(programId, request, currentUser.getUsername());
    }

    @PatchMapping("/{programId}/status")
    @PreAuthorize("hasRole('REHABILITATION_OFFICER')")
    @Operation(summary = "Update rehabilitation program status")
    public ProgramResponse updateStatus(@PathVariable UUID programId,
                                        @Valid @RequestBody UpdateProgramStatusRequest request,
                                        @AuthenticationPrincipal UserDetails currentUser) {
        return programService.updateStatus(programId, request, currentUser.getUsername());
    }

}
