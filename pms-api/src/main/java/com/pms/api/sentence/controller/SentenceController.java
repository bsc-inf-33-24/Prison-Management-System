package com.pms.api.sentence.controller;

import com.pms.api.sentence.dto.SentenceRequest;
import com.pms.api.sentence.dto.SentenceResponse;
import com.pms.api.sentence.service.SentenceService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class SentenceController {

    private final SentenceService sentenceService;

    public SentenceController(SentenceService sentenceService) {
        this.sentenceService = sentenceService;
    }

    @PostMapping("/api/sentences")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    public SentenceResponse create(@Valid @RequestBody SentenceRequest request) {
        return sentenceService.create(request);
    }

    @GetMapping("/api/sentences")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    public List<SentenceResponse> list(@RequestParam(required = false) Long inmateId,
                                      @RequestParam(required = false) Long caseId) {
        return sentenceService.list(inmateId, caseId);
    }

    @GetMapping("/api/sentences/{sentenceId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    public SentenceResponse get(@PathVariable Long sentenceId) {
        return sentenceService.get(sentenceId);
    }

    @GetMapping("/api/inmates/{inmateId}/sentences")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    public List<SentenceResponse> listForInmate(@PathVariable Long inmateId) {
        return sentenceService.list(inmateId, null);
    }

    @PutMapping("/api/sentences/{sentenceId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    public SentenceResponse update(@PathVariable Long sentenceId,
                                   @Valid @RequestBody SentenceRequest request) {
        return sentenceService.update(sentenceId, request);
    }
}
