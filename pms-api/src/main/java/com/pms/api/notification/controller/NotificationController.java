package com.pms.api.notification.controller;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.notification.dto.NotificationResponse;
import com.pms.api.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "11. Notifications", description = "Read and manage the authenticated user's in-app notifications.")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications for the authenticated user")
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal UserDetails currentUser,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.list(currentUser.getUsername(), unreadOnly, page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get the authenticated user's unread notification count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal UserDetails currentUser) {
        return new UnreadCountResponse(notificationService.unreadCount(currentUser.getUsername()));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark one of the authenticated user's notifications as read")
    public NotificationResponse markRead(@AuthenticationPrincipal UserDetails currentUser,
                                         @PathVariable Long notificationId) {
        return notificationService.markRead(currentUser.getUsername(), notificationId);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Mark all of the authenticated user's notifications as read")
    public void markAllRead(@AuthenticationPrincipal UserDetails currentUser) {
        notificationService.markAllRead(currentUser.getUsername());
    }

    public record UnreadCountResponse(long unreadCount) {
    }
}
