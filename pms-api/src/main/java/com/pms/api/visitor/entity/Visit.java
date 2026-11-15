package com.pms.api.visitor.entity;

import com.pms.api.inmate.entity.Inmate;
import com.pms.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "visit_type", nullable = false, length = 20)
    private VisitType visitType = VisitType.FAMILY;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;

    @Column(name = "visit_datetime", nullable = false)
    private OffsetDateTime startDateTime;

    @Column(name = "visit_end_datetime", nullable = false)
    private OffsetDateTime endDateTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "end_reminder_sent_at")
    private Instant endReminderSentAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logged_by", nullable = false)
    private User loggedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "visit", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private FamilyVisitDetails familyDetails;

    @OneToOne(mappedBy = "visit", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private LegalVisitDetails legalDetails;

    @OneToOne(mappedBy = "visit", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private HealthVisitDetails healthDetails;

    protected Visit() {
    }

    public Visit(Inmate inmate, OffsetDateTime startDateTime, OffsetDateTime endDateTime,
                 Integer durationMinutes, User loggedBy, VisitType visitType) {
        this(inmate, startDateTime, endDateTime, durationMinutes, loggedBy, visitType, null);
    }

    public Visit(Inmate inmate, OffsetDateTime startDateTime, OffsetDateTime endDateTime,
                 Integer durationMinutes, User loggedBy, VisitType visitType, UUID publicUuid) {
        this.inmate = inmate;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.durationMinutes = durationMinutes;
        this.loggedBy = loggedBy;
        this.visitType = visitType == null ? VisitType.FAMILY : visitType;
        if (publicUuid != null) {
            this.publicUuid = publicUuid;
        }
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

    public VisitType getVisitType() {
        return visitType;
    }

    public Inmate getInmate() {
        return inmate;
    }

    public FamilyVisitDetails getFamilyDetails() {
        return familyDetails;
    }

    public void setFamilyDetails(FamilyVisitDetails familyDetails) {
        this.familyDetails = familyDetails;
        if (familyDetails != null) {
            familyDetails.setVisit(this);
        }
    }

    public LegalVisitDetails getLegalDetails() {
        return legalDetails;
    }

    public void setLegalDetails(LegalVisitDetails legalDetails) {
        this.legalDetails = legalDetails;
        if (legalDetails != null) {
            legalDetails.setVisit(this);
        }
    }

    public HealthVisitDetails getHealthDetails() {
        return healthDetails;
    }

    public void setHealthDetails(HealthVisitDetails healthDetails) {
        this.healthDetails = healthDetails;
        if (healthDetails != null) {
            healthDetails.setVisit(this);
        }
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

    public Instant getEndReminderSentAt() {
        return endReminderSentAt;
    }

    public User getLoggedBy() {
        return loggedBy;
    }

    public void markReminderSent(Instant sentAt) {
        this.endReminderSentAt = sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
