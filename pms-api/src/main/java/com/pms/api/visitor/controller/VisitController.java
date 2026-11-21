package com.pms.api.visitor.controller;

import com.pms.api.health.dto.MedicalHistoryResponse;
import com.pms.api.health.dto.MedicalVisitResponse;
import com.pms.api.health.dto.RecordMedicalVisitRequest;
import com.pms.api.health.service.MedicalVisitService;
import com.pms.api.visitor.dto.RecordFamilyVisitRequest;
import com.pms.api.visitor.dto.RecordLegalVisitRequest;
import com.pms.api.visitor.dto.FamilyVisitHistoryResponse;
import com.pms.api.visitor.dto.FamilyVisitResponse;
import com.pms.api.visitor.dto.VisitHistoryResponse;
import com.pms.api.visitor.dto.VisitRequest;
import com.pms.api.visitor.dto.VisitResponse;
import com.pms.api.visitor.entity.VisitType;
import com.pms.api.visitor.service.VisitService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
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
@Tag(name = "09. Visits Management", description = "Manage family, legal, and medical visits.")
public class VisitController {

    private final VisitService visitService;
    private final MedicalVisitService medicalVisitService;

    public VisitController(VisitService visitService, MedicalVisitService medicalVisitService) {
        this.visitService = visitService;
        this.medicalVisitService = medicalVisitService;
    }

    @Operation(summary = "Record a family or friend visit", description = "Records a family or friend visit for the inmate and uses the authenticated account as the recording officer. Requires VISITOR_OFFICER, RECORDS_OFFICER, or SUPER_ADMIN.")
    @PostMapping("/visits/family-friends")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    public FamilyVisitResponse recordFamilyFriendsVisit(@Valid @RequestBody RecordFamilyVisitRequest request,
                                                        @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.recordFamilyVisit(request, currentUser.getUsername());
    }

    @Operation(summary = "Record a legal visit", description = "Records a legal visit for the inmate and uses the authenticated account as the recording officer. Requires CASE_OFFICER, RECORDS_OFFICER, or SUPER_ADMIN.")
    @PostMapping("/visits/legal")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CASE_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    public VisitResponse recordLegalVisit(@Valid @RequestBody RecordLegalVisitRequest request,
                                          @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.recordLegalVisit(request, currentUser.getUsername());
    }

    @Operation(summary = "Record a medical visit", description = "Records a medical visit for the inmate and uses the authenticated account as the recording officer. Requires MEDICAL_OFFICER or SUPER_ADMIN; medical details are restricted to these roles.")
    @PostMapping("/visits/medical")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDICAL_OFFICER', 'SUPER_ADMIN')")
    public MedicalVisitResponse recordMedicalVisit(@Valid @RequestBody RecordMedicalVisitRequest request,
                                                    @AuthenticationPrincipal UserDetails currentUser) {
        return medicalVisitService.recordVisit(request, currentUser.getUsername());
    }

    @Hidden
    @PostMapping("/medical-visits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MEDICAL_OFFICER', 'SUPER_ADMIN')")
    public MedicalVisitResponse recordLegacyMedicalVisit(@Valid @RequestBody RecordMedicalVisitRequest request,
                                                          @AuthenticationPrincipal UserDetails currentUser) {
        return medicalVisitService.recordVisit(request, currentUser.getUsername());
    }

    @GetMapping("/medical-visits")
    @PreAuthorize("hasAnyRole('MEDICAL_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Search an inmate's medical history", description = "Returns medical visit history for the required inmate UUID, optionally matching the inmate name. Requires MEDICAL_OFFICER or SUPER_ADMIN.")
    public MedicalHistoryResponse getMedicalHistory(@RequestParam UUID inmateId,
                                                     @RequestParam(required = false) String name) {
        return medicalVisitService.getHistory(inmateId, name);
    }

    @Hidden
    @Operation(summary = "Record a visit", description = "Record a family, legal, or health visit. Health details are restricted to Medical Officers.")
    @PostMapping("/visits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER', 'CASE_OFFICER', 'MEDICAL_OFFICER', 'SUPER_ADMIN')")
    public VisitResponse recordVisit(@Valid @RequestBody VisitRequest request,
                                    @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.recordVisit(request, currentUser.getUsername());
    }

    @Operation(summary = "Search visits", description = "Filters visits by visit UUID, visitor name, visit type, inmate UUID or name, visit date range, and recording officer UUID. Date values use ISO-8601 offset date-time. Results are paged; the recording officer is constrained by the caller's access. Requires VISITOR_OFFICER, RECORDS_OFFICER, CASE_OFFICER, MEDICAL_OFFICER, or SUPER_ADMIN.")
    @GetMapping("/visits")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER', 'CASE_OFFICER', 'MEDICAL_OFFICER', 'SUPER_ADMIN')")
    public Page<VisitResponse> searchVisits(
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) String visitorName,
            @RequestParam(required = false) String visitType,
            @RequestParam(required = false) UUID inmateId,
            @RequestParam(required = false) String inmateName,
            @RequestParam(required = false) OffsetDateTime visitDateFrom,
            @RequestParam(required = false) OffsetDateTime visitDateTo,
            @RequestParam(required = false) UUID loggedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.searchVisits(visitId, visitorName, visitType, inmateId,
                inmateName, visitDateFrom, visitDateTo, loggedBy, page, size, sort, currentUser.getUsername());
    }

    @Operation(summary = "Get an inmate's visit history", description = "Returns paged visits associated with the inmate UUID, including visitor name and visit type. Optional filters include visitor name, visit type, and inclusive ISO-8601 date bounds. Requires VISITOR_OFFICER, RECORDS_OFFICER, CASE_OFFICER, MEDICAL_OFFICER, or SUPER_ADMIN.")
    @GetMapping("/inmates/{inmateId}/visits")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER', 'CASE_OFFICER', 'MEDICAL_OFFICER', 'SUPER_ADMIN')")
    public Page<VisitHistoryResponse> getInmateVisitHistory(
            @PathVariable UUID inmateId,
            @RequestParam(required = false) String visitorName,
            @RequestParam(required = false) String visitType,
            @RequestParam(required = false) OffsetDateTime visitDateFrom,
            @RequestParam(required = false) OffsetDateTime visitDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.getInmateVisitHistory(inmateId, visitorName, visitType, visitDateFrom, visitDateTo,
                page, size, sort, currentUser.getUsername());
    }

    @Operation(summary = "Get an inmate's family and friend visits", description = "Returns paged family and friend visits for the inmate UUID, optionally filtered by visitor name and inclusive ISO-8601 date bounds. Requires VISITOR_OFFICER, RECORDS_OFFICER, or SUPER_ADMIN.")
    @GetMapping("/inmates/{inmateId}/visits/family-friends")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    public Page<FamilyVisitHistoryResponse> getInmateFamilyFriendVisits(
            @PathVariable UUID inmateId,
            @RequestParam(required = false) String visitorName,
            @RequestParam(required = false) OffsetDateTime visitDateFrom,
            @RequestParam(required = false) OffsetDateTime visitDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.getInmateFamilyVisitHistory(inmateId, visitorName,
                visitDateFrom, visitDateTo, page, size, sort, currentUser.getUsername());
    }

    @Operation(summary = "Get an inmate's legal visits", description = "Returns paged legal visits for the inmate UUID, optionally filtered by inclusive ISO-8601 date bounds. Requires CASE_OFFICER, RECORDS_OFFICER, or SUPER_ADMIN.")
    @GetMapping("/inmates/{inmateId}/visits/legal")
    @PreAuthorize("hasAnyRole('CASE_OFFICER', 'RECORDS_OFFICER', 'SUPER_ADMIN')")
    public Page<VisitHistoryResponse> getInmateLegalVisits(
            @PathVariable UUID inmateId,
            @RequestParam(required = false) OffsetDateTime visitDateFrom,
            @RequestParam(required = false) OffsetDateTime visitDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal UserDetails currentUser) {
        return visitService.getInmateVisitHistoryByType(inmateId, VisitType.LEGAL, null,
                visitDateFrom, visitDateTo, page, size, sort, currentUser.getUsername());
    }

}
