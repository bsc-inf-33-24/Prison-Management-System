package com.pms.api.user.controller;

import com.pms.api.user.dto.CreateUserRequest;
import com.pms.api.user.dto.CreateUserWithEmailRequest;
import com.pms.api.user.dto.CreateUserWithoutEmailRequest;
import com.pms.api.user.dto.UpdateUserDetailsRequest;
import com.pms.api.user.dto.UserResponse;
import com.pms.api.user.service.UserService;
import com.pms.api.common.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "2. User management", description = "Create users and update user details, status, roles, and permissions.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Email is optional. Remove the email property or set it to null for a user without an email.",
            content = @Content(schema = @Schema(oneOf = {
                            CreateUserWithEmailRequest.class,
                            CreateUserWithoutEmailRequest.class
                    }),
                    examples = {
                            @ExampleObject(name = "User with email", value = """
                                    {
                                      "fullName": "Records Officer",
                                      "username": "records01",
                                      "email": "records.officer@example.test",
                                      "password": "Records123!",
                                      "role": "Records Officer"
                                    }
                                    """),
                            @ExampleObject(name = "User without email", value = """
                                    {
                                      "fullName": "Records Officer",
                                      "username": "records01",
                                      "password": "Records123!",
                                      "role": "Records Officer"
                                    }
                                    """)
                    })))
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping
    @Operation(summary = "List users")
    public PageResponse<UserResponse> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        return userService.findAll(status, role, search, page, size, sort);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user")
    public UserResponse findById(@PathVariable UUID userId) {
        return userService.findById(userId);
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Update a user's details")
    public UserResponse updateDetails(@PathVariable UUID userId,
                                      @Valid @RequestBody UpdateUserDetailsRequest request) {
        return userService.updateDetails(userId, request);
    }

    @PatchMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate a user")
    public UserResponse deactivate(@PathVariable UUID userId) {
        return userService.deactivate(userId);
    }

    @PatchMapping("/{userId}/reactivate")
    @Operation(summary = "Reactivate a user")
    public UserResponse reactivate(@PathVariable UUID userId) {
        return userService.reactivate(userId);
    }

}
