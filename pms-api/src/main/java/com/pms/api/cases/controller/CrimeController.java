package com.pms.api.cases.controller;

import com.pms.api.cases.dto.CrimeRequest;
import com.pms.api.cases.dto.CrimeResponse;
import com.pms.api.cases.service.CrimeService;
import com.pms.api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Case management", description = "Maintain the offense catalog used across case and sentence records.")
public class CrimeController {

    private final CrimeService crimeService;

    public CrimeController(CrimeService crimeService) {
        this.crimeService = crimeService;
    }

    @Operation(summary = "Create a crime catalog entry")
    @ApiResponse(responseCode = "201", description = "Crime created")
    @PostMapping("/crimes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CrimeResponse create(@Valid @RequestBody CrimeRequest request) {
        return crimeService.create(request);
    }

    @Operation(summary = "List crime catalog entries")
    @GetMapping("/crimes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CASE_OFFICER')")
    public PageResponse<CrimeResponse> list(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) String search,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size,
                                            @RequestParam(required = false) List<String> sort) {
        return crimeService.list(category, search, page, size, sort);
    }

    @Operation(summary = "Get a crime catalog entry")
    @GetMapping("/crimes/{crimeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CASE_OFFICER')")
    public CrimeResponse get(@PathVariable UUID crimeId) {
        return crimeService.get(crimeId);
    }

    @Operation(summary = "Update a crime catalog entry")
    @PatchMapping("/crimes/{crimeId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CrimeResponse update(@PathVariable UUID crimeId, @Valid @RequestBody CrimeRequest request) {
        return crimeService.update(crimeId, request);
    }
}
