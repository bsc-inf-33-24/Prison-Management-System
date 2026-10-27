package com.pms.api.inmate.controller;

import com.pms.api.inmate.dto.InmateRequest;
import com.pms.api.inmate.dto.UpdateInmateDetailsRequest;
import com.pms.api.inmate.dto.InmateResponse;
import com.pms.api.inmate.service.InmateService;
import com.pms.api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inmates")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "3. Inmate enrollment and inmate records", description = "Enroll inmates and manage inmate records and custody information.")
public class InmateController {

    private final InmateService inmateService;

    public InmateController(InmateService inmateService) {
        this.inmateService = inmateService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Enroll an inmate",
            description = "Creates the inmate record and allocates the inmate to the selected cell in one transaction.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inmate created and allocated to the selected cell."),
            @ApiResponse(responseCode = "409", description = "The selected cell is full."),
            @ApiResponse(responseCode = "404", description = "The selected cell does not exist.")
    })
    public InmateResponse enroll(@Valid @RequestBody InmateRequest request) {
        return inmateService.enroll(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "List inmate records")
    public PageResponse<InmateResponse> list(@RequestParam(required = false) String search,
                                             @RequestParam(required = false) String gender,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size,
                                             @RequestParam(required = false) List<String> sort) {
        return inmateService.list(search, gender, page, size, sort);
    }

    @GetMapping("/{inmateId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Get an inmate record")
    public InmateResponse get(@PathVariable UUID inmateId) {
        return inmateService.get(inmateId);
    }

    @PatchMapping("/{inmateId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'SUPER_ADMIN')")
    @Operation(summary = "Update an inmate record",
            description = "Updates inmate details.")
    public InmateResponse update(@PathVariable UUID inmateId,
                                 @Valid @RequestBody UpdateInmateDetailsRequest request) {
        return inmateService.update(inmateId, request);
    }
}
