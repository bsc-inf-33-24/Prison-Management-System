package com.pms.api.notification.repository;

import com.pms.api.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(
            Long recipientUserId, boolean isRead, Pageable pageable);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    long countByRecipientUserIdAndIsRead(Long recipientUserId, boolean isRead);

    @Modifying
    @Query("""
            update Notification notification
            set notification.isRead = true, notification.readAt = :readAt
            where notification.recipientUserId = :recipientUserId
                and notification.isRead = false
            """)
    int markAllRead(@Param("recipientUserId") Long recipientUserId, @Param("readAt") Instant readAt);
}
