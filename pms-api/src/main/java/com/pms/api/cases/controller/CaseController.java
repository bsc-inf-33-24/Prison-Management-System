package com.pms.api.cases.controller;

import com.pms.api.cases.dto.CaseRequest;
import com.pms.api.cases.dto.CaseResponse;
import com.pms.api.cases.dto.CaseStatusRequest;
import com.pms.api.cases.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Case management", description = "Create and manage inmate case records and case progression.")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping("/cases")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CASE_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Create a case record")
    public CaseResponse createCase(@Valid @RequestBody CaseRequest request,
                                  @AuthenticationPrincipal UserDetails user) {
        return caseService.createCase(request, user.getUsername());
    }

    @GetMapping("/cases")
    @PreAuthorize("hasAnyRole('CASE_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List case records")
    public List<CaseResponse> listCases(@RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long inmateId,
                                       @RequestParam(required = false) Long crimeId) {
        return caseService.listCases(status, inmateId, crimeId);
    }

    @GetMapping("/cases/{caseId}")
    @PreAuthorize("hasAnyRole('CASE_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Get a case record")
    public CaseResponse getCase(@PathVariable Long caseId) {
        return caseService.getCase(caseId);
    }

    @PutMapping("/cases/{caseId}")
    @PreAuthorize("hasAnyRole('CASE_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Update a case record")
    public CaseResponse updateCase(@PathVariable Long caseId, @Valid @RequestBody CaseRequest request) {
        return caseService.updateCase(caseId, request);
    }

    @PatchMapping("/cases/{caseId}/status")
    @PreAuthorize("hasAnyRole('CASE_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Update case status")
    public CaseResponse updateStatus(@PathVariable Long caseId, @Valid @RequestBody CaseStatusRequest request) {
        return caseService.updateStatus(caseId, request);
    }

    @GetMapping("/inmates/{inmateId}/cases")
    @PreAuthorize("hasAnyRole('CASE_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List inmate cases")
    public List<CaseResponse> getCasesForInmate(@PathVariable Long inmateId) {
        return caseService.getCasesForInmate(inmateId);
    }
}
