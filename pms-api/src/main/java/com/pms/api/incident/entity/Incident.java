package com.pms.api.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

// Maps directly onto V5__discipline_tables_creation.sql's "Incident" table.
// Column-for-column with that migration on purpose — see IncidentInmate for
// the relationship table (IncidentInmate), which carries role_in_incident.
//
// Deliberately does NOT extend BaseEntity / have created_at, updated_at, or
// a foreign key to User: V5 doesn't define those columns, and
// spring.jpa.hibernate.ddl-auto=validate means Hibernate refuses to start if
// the entity claims a column the database doesn't have.
@Entity
@Table(name = "Incident")
public class Incident {

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_RESOLVED = "resolved";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incident_id")
    private Long id;

    @Column(name = "incident_date")
    private LocalDate incidentDate;

    @Column(name = "description")
    private String description;

    // Restricted to 'open' / 'resolved' by a CHECK constraint in V5.
    @Column(name = "status", length = 50)
    private String status;

    // V5 caps this at VARCHAR(255) - deliberately narrower than you might
    // expect for free text, so RecordOutcomeRequest validates the same limit.
    @Column(name = "outcome", length = 255)
    private String outcome;

    // Plain text in the schema (not a foreign key to the users table). The
    // service layer still derives this from the authenticated JWT identity
    // rather than trusting client input - it's just stored as a name, not a
    // referential link.
    @Column(name = "reported_by", length = 255)
    private String reportedBy;

    // JPA requires a no-arg constructor; kept protected so nothing outside
    // this package can create a half-built Incident.
    protected Incident() {
    }

    private Incident(LocalDate incidentDate, String description, String reportedBy) {
        this.incidentDate = incidentDate;
        this.description = description;
        this.reportedBy = reportedBy;
        this.status = STATUS_OPEN;
    }

    // Named factory method instead of a public constructor - reads clearly
    // at the call site (Incident.logNew(...)) and keeps the "always starts
    // open" rule enforceable in one place.
    public static Incident logNew(LocalDate incidentDate, String description, String reportedBy) {
        return new Incident(incidentDate, description, reportedBy);
    }

    public Long getId() {
        return id;
    }

    public LocalDate getIncidentDate() {
        return incidentDate;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getReportedBy() {
        return reportedBy;
    }
}
