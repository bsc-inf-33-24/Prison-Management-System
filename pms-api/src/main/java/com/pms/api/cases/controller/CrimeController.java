package com.pms.api.cases.controller;

import com.pms.api.cases.dto.CrimeRequest;
import com.pms.api.cases.dto.CrimeResponse;
import com.pms.api.cases.service.CrimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
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
    public List<CrimeResponse> list() {
        return crimeService.list();
    }

    @Operation(summary = "Get a crime catalog entry")
    @GetMapping("/crimes/{crimeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CASE_OFFICER')")
    public CrimeResponse get(@PathVariable Long crimeId) {
        return crimeService.get(crimeId);
    }

    @Operation(summary = "Update a crime catalog entry")
    @PutMapping("/crimes/{crimeId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CrimeResponse update(@PathVariable Long crimeId, @Valid @RequestBody CrimeRequest request) {
        return crimeService.update(crimeId, request);
    }
}
