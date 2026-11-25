package com.pms.api.inmate.controller;

import com.pms.api.inmate.dto.InmateRequest;
import com.pms.api.inmate.dto.UpdateInmateDetailsRequest;
import com.pms.api.inmate.dto.PatchRemandRequest;
import com.pms.api.inmate.dto.InmateResponse;
import com.pms.api.inmate.service.InmateService;
import com.pms.api.inmate.service.ReadmissionService;
import com.pms.api.inmate.dto.CustodyStayResponse;
import com.pms.api.inmate.dto.ReadmissionRequest;
import com.pms.api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inmates")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "04. Inmate enrollment and inmate records", description = "Enroll inmates and manage inmate records and custody information.")
public class InmateController {

    private final InmateService inmateService;
    private final ReadmissionService readmissionService;

    public InmateController(InmateService inmateService, ReadmissionService readmissionService) {
        this.inmateService = inmateService;
        this.readmissionService = readmissionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @Operation(summary = "Enroll an inmate",
            description = "Creates the inmate at the authenticated Records Officer's assigned prison and allocates the inmate to a cell in that prison in one transaction. Requires RECORDS_OFFICER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inmate created and allocated to the selected cell."),
            @ApiResponse(responseCode = "409", description = "The selected cell is full."),
            @ApiResponse(responseCode = "404", description = "The selected cell does not exist.")
    })
    public InmateResponse enroll(@Valid @RequestBody InmateRequest request) {
        return inmateService.enroll(request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @Operation(summary = "Enroll an inmate with a photo",
            description = "Accepts the enrollment JSON and an optional JPEG or PNG photo in one multipart request. Requires RECORDS_OFFICER.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = com.pms.api.inmate.dto.InmatePhotoEnrollmentRequest.class),
                    encoding = @Encoding(name = "inmate", contentType = MediaType.APPLICATION_JSON_VALUE))))
    public InmateResponse enrollWithPhoto(@Valid @RequestPart("inmate") InmateRequest request,
                                          @RequestPart(value = "photo", required = false) MultipartFile photo,
                                          @AuthenticationPrincipal UserDetails currentUser) {
        return inmateService.enroll(request, photo, currentUser.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'COMMISSIONER', 'TRANSFER_OFFICER')")
    @Operation(summary = "List inmate records", description = "Returns paged inmate records. Search text and gender are optional filters; page, size, and sort control pagination. Transfer Officers can view records across all prisons.")
    public PageResponse<InmateResponse> list(@RequestParam(required = false) String search,
                                             @RequestParam(required = false) String gender,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size,
                                             @RequestParam(required = false) List<String> sort) {
        return inmateService.list(search, gender, page, size, sort);
    }

    @GetMapping("/{inmateId}")
    @PreAuthorize("hasAnyRole('RECORDS_OFFICER', 'COMMISSIONER', 'TRANSFER_OFFICER')")
    @Operation(summary = "Get an inmate record", description = "Returns the inmate record identified by its public UUID. Transfer Officers can view records across all prisons.")
    public InmateResponse get(@PathVariable UUID inmateId) {
        return inmateService.get(inmateId);
    }

    @PatchMapping("/{inmateId}")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @Operation(summary = "Update an inmate record",
            description = "Updates the supplied inmate details for the inmate public UUID. Requires RECORDS_OFFICER.")
    public InmateResponse update(@PathVariable UUID inmateId,
                                 @Valid @RequestBody UpdateInmateDetailsRequest request) {
        return inmateService.update(inmateId, request);
    }

    @PatchMapping("/{inmateId}/remand")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @Operation(summary = "Update remand details", description = "Updates the expected court date for the remand inmate public UUID. Requires RECORDS_OFFICER.")
    public InmateResponse updateRemand(@PathVariable UUID inmateId,
                                       @Valid @RequestBody PatchRemandRequest request) {
        return inmateService.updateRemand(inmateId, request);
    }

    @PostMapping("/{prisonerId}/readmissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @Operation(summary = "Readmit an existing inmate", description = "Opens a new custody stay for a previously discharged inmate using the supplied readmission details. Requires RECORDS_OFFICER.")
    public CustodyStayResponse readmit(@PathVariable UUID prisonerId,
                                       @Valid @RequestBody ReadmissionRequest request) {
        return readmissionService.readmit(prisonerId, request);
    }

    @PostMapping("/{prisonerId}/discharge")
    @PreAuthorize("hasRole('RECORDS_OFFICER')")
    @Operation(summary = "Discharge an inmate", description = "Closes the inmate's active custody stay. The path value is the inmate public UUID. Requires RECORDS_OFFICER.")
    public CustodyStayResponse discharge(@PathVariable UUID prisonerId) {
        return readmissionService.discharge(prisonerId);
    }
}
