package com.pms.api.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_record_id", length = 255)
    private String relatedRecordId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public Notification(Long recipientUserId, String type, String title, String message,
                        String relatedRecordId) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedRecordId = relatedRecordId;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getRelatedRecordId() {
        return relatedRecordId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead(Instant readAt) {
        if (!isRead) {
            isRead = true;
            this.readAt = readAt;
        }
    }

    @PrePersist
    private void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
