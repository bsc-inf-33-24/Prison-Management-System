package com.pms.api.visitor.controller;

import com.pms.api.visitor.dto.VisitorRequest;
import com.pms.api.visitor.dto.VisitorResponse;
import com.pms.api.visitor.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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
public class VisitorController {

    private final VisitorService visitorService;

    public VisitorController(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @Operation(summary = "Register a visitor", description = "Create a visitor record using the authenticated Visitor Officer account.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Visitor created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "409", description = "Duplicate identity number")
    })
    @PostMapping("/visitors")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VISITOR_OFFICER')")
    public VisitorResponse registerVisitor(@Valid @RequestBody VisitorRequest request,
                                          @AuthenticationPrincipal UserDetails currentUser) {
        return visitorService.registerVisitor(request, currentUser.getUsername());
    }

    @Operation(summary = "Get a visitor by ID", description = "Returns the identified visitor record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visitor found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Visitor not found")
    })
    @GetMapping("/visitors/{visitorId}")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER')")
    public VisitorResponse getVisitor(@PathVariable Long visitorId) {
        return visitorService.getVisitor(visitorId);
    }

    @Operation(summary = "Search visitors", description = "List and filter visitor records with pagination and sorting.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed")
    })
    @GetMapping("/visitors")
    @PreAuthorize("hasAnyRole('VISITOR_OFFICER', 'RECORDS_OFFICER')")
    public Page<VisitorResponse> searchVisitors(
            @Parameter(description = "Exact or partial visitor name") @RequestParam(required = false) String fullName,
            @Parameter(description = "Identity number filter") @RequestParam(required = false) String idNumber,
            @Parameter(description = "Phone filter") @RequestParam(required = false) String phone,
            @Parameter(description = "Relationship filter") @RequestParam(required = false) String relationshipToInmate,
            @Parameter(description = "Page number starting at 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, max 100") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction, e.g. fullName,asc") @RequestParam(required = false) String sort) {
        return visitorService.searchVisitors(fullName, idNumber, phone, relationshipToInmate, page, size, sort);
    }
}
