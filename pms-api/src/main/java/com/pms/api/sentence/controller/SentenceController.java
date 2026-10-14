package com.pms.api.sentence.controller;

import com.pms.api.sentence.dto.SentenceRequest;
import com.pms.api.sentence.dto.SentenceResponse;
import com.pms.api.sentence.service.SentenceService;
import com.pms.api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
@Tag(name = "Sentence management", description = "Create, review, and update inmate sentence records.")
public class SentenceController {

    private final SentenceService sentenceService;

    public SentenceController(SentenceService sentenceService) {
        this.sentenceService = sentenceService;
    }

    @PostMapping("/sentences")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Create a sentence record")
    public SentenceResponse create(@Valid @RequestBody SentenceRequest request) {
        return sentenceService.create(request);
    }

    @GetMapping("/sentences")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List sentence records")
    public PageResponse<SentenceResponse> list(@RequestParam(required = false) Long inmateId,
                                               @RequestParam(required = false) Long caseId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) LocalDate startDateFrom,
                                               @RequestParam(required = false) LocalDate startDateTo,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size,
                                               @RequestParam(required = false) List<String> sort) {
        return sentenceService.list(inmateId, caseId, status, startDateFrom, startDateTo, page, size, sort);
    }

    @GetMapping("/sentences/{sentenceId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Get a sentence record")
    public SentenceResponse get(@PathVariable Long sentenceId) {
        return sentenceService.get(sentenceId);
    }

    @GetMapping("/inmates/{inmateId}/sentences")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List sentences for an inmate")
    public PageResponse<SentenceResponse> listForInmate(@PathVariable Long inmateId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) LocalDate startDateFrom,
                                                        @RequestParam(required = false) LocalDate startDateTo,
                                                        @RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size,
                                                        @RequestParam(required = false) List<String> sort) {
        return sentenceService.list(inmateId, null, status, startDateFrom, startDateTo, page, size, sort);
    }

    @PutMapping("/sentences/{sentenceId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'CASE_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Update a sentence record")
    public SentenceResponse update(@PathVariable Long sentenceId,
                                   @Valid @RequestBody SentenceRequest request) {
        return sentenceService.update(sentenceId, request);
    }
}
