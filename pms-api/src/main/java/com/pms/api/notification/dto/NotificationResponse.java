package com.pms.api.notification.dto;

import com.pms.api.notification.entity.Notification;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String relatedRecordId,
        Instant createdAt,
        boolean isRead,
        Instant readAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedRecordId(),
                notification.getCreatedAt(),
                notification.isRead(),
                notification.getReadAt());
    }
}
