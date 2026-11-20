package com.pms.api.sentence.controller;

import com.pms.api.sentence.dto.RemissionResponse;
import com.pms.api.sentence.service.SentenceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "09. Sentences and remission", description = "Manage sentence remission records.")
public class RemissionController {
    private final SentenceService service;

    public RemissionController(SentenceService service) { this.service = service; }

    @GetMapping("/sentences/{sentenceId}/remissions")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public List<RemissionResponse> list(@PathVariable UUID sentenceId) {
        return service.listRemissions(sentenceId);
    }

}
