package com.pms.api.health.controller;

import com.pms.api.health.dto.MedicalHistoryResponse;
import com.pms.api.health.dto.MedicalVisitResponse;
import com.pms.api.health.dto.RecordMedicalVisitRequest;
import com.pms.api.health.service.MedicalVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medical-visits")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "6. Medical visits", description = "Record medical visits and review an inmate's medical history.")
public class MedicalVisitController {

    private final MedicalVisitService medicalVisitService;

    public MedicalVisitController(MedicalVisitService medicalVisitService) {
        this.medicalVisitService = medicalVisitService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDICAL_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Record an inmate medical visit")
    public MedicalVisitResponse recordVisit(@Valid @RequestBody RecordMedicalVisitRequest request,
                                             @AuthenticationPrincipal UserDetails currentUser) {
        return medicalVisitService.recordVisit(request, currentUser.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICAL_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Search for an inmate's medical history by inmate ID and optional name")
    public MedicalHistoryResponse getHistory(@RequestParam UUID inmateId,
                                             @RequestParam(required = false) String name) {
        return medicalVisitService.getHistory(inmateId, name);
    }
}
