package com.pms.api.inmate.controller;

import com.pms.api.inmate.dto.InmateRequest;
import com.pms.api.inmate.dto.InmateResponse;
import com.pms.api.inmate.service.InmateService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inmates")
@SecurityRequirement(name = "bearerAuth")
public class InmateController {

    private final InmateService inmateService;

    public InmateController(InmateService inmateService) {
        this.inmateService = inmateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public InmateResponse enroll(@Valid @RequestBody InmateRequest request) {
        return inmateService.enroll(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    public List<InmateResponse> list(@RequestParam(required = false) String search) {
        return inmateService.list(search);
    }

    @GetMapping("/{inmateId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    public InmateResponse get(@PathVariable Long inmateId) {
        return inmateService.get(inmateId);
    }

    @PutMapping("/{inmateId}")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    public InmateResponse update(@PathVariable Long inmateId, @Valid @RequestBody InmateRequest request) {
        return inmateService.update(inmateId, request);
    }
}
