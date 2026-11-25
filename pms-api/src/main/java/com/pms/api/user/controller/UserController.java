package com.pms.api.user.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pms.api.common.error.ApiError;
import com.pms.api.common.pagination.PageResponse;
import com.pms.api.user.dto.CreateUserRequest;
import com.pms.api.user.dto.UpdateUserDetailsRequest;
import com.pms.api.user.dto.UserResponse;
import com.pms.api.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "03. User management", description = "Super Admins manage user accounts; Commissioners can list accounts.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user", description = "Creates a staff account and emails a generated temporary password to its required email address. Requires SUPER_ADMIN.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Email is required. The system emails a generated temporary password; the request never accepts a password.",
            content = @Content(schema = @Schema(implementation = CreateUserRequest.class),
                examples = @ExampleObject(name = "Create user", value = """
                    {
                      "fullName": "Records Officer",
                      "username": "records01",
                      "email": "records.officer@example.test",
                      "role": "Records Officer",
                      "prisonId": "9b91b52d-d631-4bea-a1df-f8b66473dc0a"
                    }
                    """))))
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created.",
                content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Required fields, email, or role is invalid.",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required.",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Only a Super Admin may create users.",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "The username or email already exists.",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMMISSIONER')")
    @Operation(summary = "List users", description = "Returns users matching optional status, role, search text, and prison UUID filters. Results are paged and sorted. Requires SUPER_ADMIN or COMMISSIONER.")
    public PageResponse<UserResponse> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID prisonId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return userService.findAll(status, role, search, prisonId, page, size, sort);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get a user", description = "Returns the user identified by public UUID. Requires SUPER_ADMIN.")
    public UserResponse findById(@PathVariable UUID userId) {
        return userService.findById(userId);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update a user's details", description = "Updates the supplied user details, including email or prison assignment. Requires SUPER_ADMIN.")
    public UserResponse updateDetails(@PathVariable UUID userId,
                                      @Valid @RequestBody UpdateUserDetailsRequest request) {
        return userService.updateDetails(userId, request);
    }

    @PatchMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Deactivate a user", description = "Deactivates the user identified by public UUID and prevents further sign-in. Requires SUPER_ADMIN.")
    public UserResponse deactivate(@PathVariable UUID userId) {
        return userService.deactivate(userId);
    }

    @PatchMapping("/{userId}/reactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reactivate a user", description = "Reactivates the user identified by public UUID, allowing sign-in subject to normal authentication rules. Requires SUPER_ADMIN.")
    public UserResponse reactivate(@PathVariable UUID userId) {
        return userService.reactivate(userId);
    }

}
