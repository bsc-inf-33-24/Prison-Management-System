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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;

    @Column(name = "visit_datetime", nullable = false)
    private OffsetDateTime visitDateTime;

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

    public Visit(Visitor visitor, Inmate inmate, OffsetDateTime visitDateTime, Integer durationMinutes, User loggedBy) {
        this.visitor = visitor;
        this.inmate = inmate;
        this.visitDateTime = visitDateTime;
        this.durationMinutes = durationMinutes;
        this.loggedBy = loggedBy;
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

    public Visitor getVisitor() {
        return visitor;
    }

    public Inmate getInmate() {
        return inmate;
    }

    public OffsetDateTime getVisitDateTime() {
        return visitDateTime;
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

    public void updateDetails(OffsetDateTime visitDateTime, Integer durationMinutes) {
        this.visitDateTime = visitDateTime;
        this.durationMinutes = durationMinutes;
    }
}
