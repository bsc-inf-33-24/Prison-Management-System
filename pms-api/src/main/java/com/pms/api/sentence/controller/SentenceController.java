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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sentences")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "09. Sentences and remission", description = "Manage inmate sentences and their remission records.")
public class SentenceController {
    private final SentenceService service;
    public SentenceController(SentenceService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public SentenceResponse create(@Valid @RequestBody CreateSentenceRequest request) { return service.create(request); }

    @GetMapping("/{sentenceId}")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public SentenceResponse get(@PathVariable UUID sentenceId) { return service.get(sentenceId); }

    @PatchMapping("/{sentenceId}")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public SentenceResponse update(@PathVariable UUID sentenceId,
            @Valid @RequestBody PatchSentenceRequest request) {
        return service.update(sentenceId, request);
    }

    @GetMapping
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public List<SentenceResponse> list(@RequestParam UUID inmateId) { return service.list(inmateId); }

}
