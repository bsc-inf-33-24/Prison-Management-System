package com.pms.api.incident.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

// Extends BaseEntity so we get created_at / updated_at for free (see
// com.pms.api.common.entity.BaseEntity). BaseEntity does NOT provide the id,
// so every entity (including this one) declares its own @Id — that's the
// convention the rest of the project already uses (see Inmate.java).
@Entity
@Table(name = "incidents")
public class Incident extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // When the incident actually happened (as reported), separate from
    // createdAt (when the record was saved to the system, from BaseEntity).
    @Column(name = "incident_date", nullable = false)
    private OffsetDateTime incidentDate;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    // Null until a Disciplinary Officer records one via PATCH .../outcome.
    @Column(name = "outcome", length = 2000)
    private String outcome;

    // Who logged this incident. Required, and never changes after creation
    // (updatable = false), so the audit trail of "who reported it" is fixed.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logged_by", nullable = false, updatable = false)
    private User loggedBy;

    // The inmate(s) involved. Many-to-many because one incident can involve
    // several inmates, and one inmate can be in several incidents over time.
    @ManyToMany
    @JoinTable(
            name = "incident_inmates",
            joinColumns = @JoinColumn(name = "incident_id"),
            inverseJoinColumns = @JoinColumn(name = "inmate_id")
    )
    private Set<Inmate> inmates = new HashSet<>();

    // JPA needs a no-arg constructor; keep it protected so nobody outside
    // this class/package accidentally creates a half-built Incident.
    protected Incident() {
    }

    private Incident(OffsetDateTime incidentDate, String description, User loggedBy, Set<Inmate> inmates) {
        this.incidentDate = incidentDate;
        this.description = description;
        this.loggedBy = loggedBy;
        this.inmates = inmates;
    }

    // Named factory method instead of a public constructor — reads clearly
    // at the call site (Incident.logNew(...)) and keeps construction rules
    // enforceable in one place if we add more later.
    public static Incident logNew(OffsetDateTime incidentDate, String description, User loggedBy, Set<Inmate> inmates) {
        return new Incident(incidentDate, description, loggedBy, inmates);
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getIncidentDate() {
        return incidentDate;
    }

    public String getDescription() {
        return description;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public User getLoggedBy() {
        return loggedBy;
    }

    public Set<Inmate> getInmates() {
        return inmates;
    }
}