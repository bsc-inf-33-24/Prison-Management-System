package com.pms.api.visitor.entity;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "visits")
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visit_id")
    private Long id;

    @Column(name = "public_uuid", nullable = false, updatable = false, unique = true)
    private UUID publicUuid = UUID.randomUUID();

    @Column(name = "visitor_full_name", nullable = false, length = 255)
    private String visitorFullName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;

    @Column(name = "visit_datetime", nullable = false)
    private OffsetDateTime startDateTime;

    @Column(name = "visit_end_datetime", nullable = false)
    private OffsetDateTime endDateTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logged_by", nullable = false)
    private User loggedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Visit() {
    }

    public Visit(Inmate inmate, OffsetDateTime startDateTime, OffsetDateTime endDateTime,
                 Integer durationMinutes, User loggedBy,
                 String visitorFullName) {
        this.inmate = inmate;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.durationMinutes = durationMinutes;
        this.loggedBy = loggedBy;
        this.visitorFullName = visitorFullName;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicUuid() {
        return publicUuid;
    }

    public String getVisitorFullName() {
        return visitorFullName;
    }

    public Inmate getInmate() {
        return inmate;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public User getLoggedBy() {
        return loggedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(OffsetDateTime startDateTime, OffsetDateTime endDateTime, Integer durationMinutes) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.durationMinutes = durationMinutes;
    }
}
