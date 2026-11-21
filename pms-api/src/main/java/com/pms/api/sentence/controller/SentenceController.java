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
    @io.swagger.v3.oas.annotations.Operation(summary = "Create an inmate sentence",
            description = "Records a sentence for an inmate. Requires the RECORDS_OFFICER role.")
    public SentenceResponse create(@Valid @RequestBody CreateSentenceRequest request) { return service.create(request); }

    @GetMapping("/{sentenceId}")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get a sentence",
            description = "Returns the sentence identified by its public UUID. Requires the RECORDS_OFFICER role.")
    public SentenceResponse get(@PathVariable UUID sentenceId) { return service.get(sentenceId); }

    @PatchMapping("/{sentenceId}")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @io.swagger.v3.oas.annotations.Operation(summary = "Update a sentence",
            description = "Updates the supplied sentence fields. Requires the RECORDS_OFFICER role.")
    public SentenceResponse update(@PathVariable UUID sentenceId,
            @Valid @RequestBody PatchSentenceRequest request) {
        return service.update(sentenceId, request);
    }

    @GetMapping
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @io.swagger.v3.oas.annotations.Operation(summary = "List sentences for an inmate",
            description = "Returns all sentences associated with the required inmate UUID. Requires the RECORDS_OFFICER role.")
    public List<SentenceResponse> list(@RequestParam UUID inmateId) { return service.list(inmateId); }

}
