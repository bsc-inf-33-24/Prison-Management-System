package com.pms.api.notification.service;

import com.pms.api.common.pagination.PageResponse;
import com.pms.api.notification.dto.NotificationResponse;
import com.pms.api.notification.entity.Notification;
import com.pms.api.notification.repository.NotificationRepository;
import com.pms.api.user.entity.User;
import com.pms.api.user.repository.UserRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final String ACTIVE_STATUS = "active";
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notifyRoles(String type, String title, String message, UUID relatedRecordId,
                            Set<String> roles) {
        List<User> recipients = userRepository.findByRoleInAndStatus(roles, ACTIVE_STATUS);
        saveForRecipients(recipients, type, title, message, relatedRecordId);
    }

    @Transactional
    public void notifyUser(Long recipientUserId, String type, String title, String message,
                           UUID relatedRecordId) {
        notificationRepository.save(new Notification(
                recipientUserId, type, title, message,
                relatedRecordId == null ? null : relatedRecordId.toString()));
    }

    @Transactional
    public void notifyUserAndRoles(User target, String type, String title, String message,
                                   UUID relatedRecordId, Set<String> roles) {
        LinkedHashSet<User> recipients = new LinkedHashSet<>();
        if (target.isActive()) {
            recipients.add(target);
        }
        recipients.addAll(userRepository.findByRoleInAndStatus(roles, ACTIVE_STATUS));
        saveForRecipients(recipients, type, title, message, relatedRecordId);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(String username, boolean unreadOnly, int page, int size) {
        validatePage(page, size);
        User recipient = findUser(username);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByRecipientUserIdAndIsReadOrderByCreatedAtDesc(
                        recipient.getId(), false, pageable)
                : notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipient.getId(), pageable);
        return PageResponse.from(notifications.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return notificationRepository.countByRecipientUserIdAndIsRead(findUser(username).getId(), false);
    }

    @Transactional
    public NotificationResponse markRead(String username, Long notificationId) {
        User recipient = findUser(username);
        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(notificationId, recipient.getId())
                .orElseThrow(NotificationNotFoundException::new);
        notification.markRead(Instant.now());
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead(String username) {
        return notificationRepository.markAllRead(findUser(username).getId(), Instant.now());
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElseThrow(NotificationUserNotFoundException::new);
    }

    private void saveForRecipients(Collection<User> recipients, String type, String title,
                                   String message, UUID relatedRecordId) {
        LinkedHashSet<Long> recipientIds = new LinkedHashSet<>();
        for (User recipient : recipients) {
            if (recipient.isActive() && recipientIds.add(recipient.getId())) {
                notificationRepository.save(new Notification(
                        recipient.getId(), type, title, message,
                        relatedRecordId == null ? null : relatedRecordId.toString()));
            }
        }
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
    }

    public static class NotificationNotFoundException extends RuntimeException {
    }

    public static class NotificationUserNotFoundException extends RuntimeException {
    }
}
