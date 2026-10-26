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

    @Column(name = "visitor_id_number", nullable = false, length = 255)
    private String visitorIdNumber;

    @Column(name = "visitor_phone", length = 50)
    private String visitorPhone;

    @Column(name = "visitor_relationship_to_inmate", nullable = false, length = 255)
    private String visitorRelationshipToInmate;

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

    public Visit(Inmate inmate, OffsetDateTime visitDateTime, Integer durationMinutes, User loggedBy,
                 String visitorFullName, String visitorIdNumber, String visitorPhone,
                 String visitorRelationshipToInmate) {
        this.inmate = inmate;
        this.visitDateTime = visitDateTime;
        this.durationMinutes = durationMinutes;
        this.loggedBy = loggedBy;
        this.visitorFullName = visitorFullName;
        this.visitorIdNumber = visitorIdNumber;
        this.visitorPhone = visitorPhone;
        this.visitorRelationshipToInmate = visitorRelationshipToInmate;
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

    public String getVisitorIdNumber() {
        return visitorIdNumber;
    }

    public String getVisitorPhone() {
        return visitorPhone;
    }

    public String getVisitorRelationshipToInmate() {
        return visitorRelationshipToInmate;
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

    public void updateDetails(OffsetDateTime visitDateTime, Integer durationMinutes,
                              String visitorFullName, String visitorIdNumber, String visitorPhone,
                              String visitorRelationshipToInmate) {
        this.visitDateTime = visitDateTime;
        this.durationMinutes = durationMinutes;
        this.visitorFullName = visitorFullName;
        this.visitorIdNumber = visitorIdNumber;
        this.visitorPhone = visitorPhone;
        this.visitorRelationshipToInmate = visitorRelationshipToInmate;
    }
}
