package com.pms.api.user.controller;

import com.pms.api.user.dto.CreateUserRequest;
import com.pms.api.user.dto.UpdateRoleRequest;
import com.pms.api.user.dto.UserResponse;
import com.pms.api.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{userId}")
    public UserResponse findById(@PathVariable Long userId) {
        return userService.findById(userId);
    }

    @PatchMapping("/{userId}/deactivate")
    public UserResponse deactivate(@PathVariable Long userId) {
        return userService.deactivate(userId);
    }

    @PatchMapping("/{userId}/reactivate")
    public UserResponse reactivate(@PathVariable Long userId) {
        return userService.reactivate(userId);
    }

    @PutMapping("/{userId}/role")
    public UserResponse updateRole(@PathVariable Long userId, @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateRole(userId, request);
    }

}
