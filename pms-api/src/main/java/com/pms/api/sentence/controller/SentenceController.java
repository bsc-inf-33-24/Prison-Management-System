package com.pms.api.sentence.controller;

import com.pms.api.sentence.dto.*;
import com.pms.api.sentence.service.SentenceService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "09. Sentences and remission", description = "Manage inmate sentences and their remission records.")
public class SentenceController {
    private final SentenceService service;
    public SentenceController(SentenceService service) { this.service = service; }

    @PostMapping("/sentences")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'PRISON_OFFICER')")
    public SentenceResponse create(@Valid @RequestBody CreateSentenceRequest request) { return service.create(request); }

    @GetMapping("/inmates/{inmateId}/sentences")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public List<SentenceResponse> list(@PathVariable UUID inmateId) { return service.list(inmateId); }

    @PatchMapping("/sentences/{sentenceId}/status")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'PRISON_OFFICER')")
    public SentenceResponse updateStatus(@PathVariable UUID sentenceId,
            @Valid @RequestBody UpdateSentenceStatusRequest request) {
        return service.updateStatus(sentenceId, request.status());
    }

    @PostMapping("/sentences/{sentenceId}/remissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'PRISON_OFFICER')")
    public RemissionResponse addRemission(@PathVariable UUID sentenceId,
            @Valid @RequestBody CreateRemissionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return service.addRemission(sentenceId, request, user.getUsername());
    }

    @GetMapping("/sentences/{sentenceId}/remissions")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public List<RemissionResponse> listRemissions(@PathVariable UUID sentenceId) { return service.listRemissions(sentenceId); }
}
