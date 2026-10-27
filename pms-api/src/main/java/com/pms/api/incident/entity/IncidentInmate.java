package com.pms.api.incident.entity;

import com.pms.api.inmate.entity.Inmate;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.UUID;

// Maps V5__discipline_tables_creation.sql's "IncidentInmate" table: the
// relationship between an Incident and the inmate(s) involved, carrying the
// one extra column the schema defines - role_in_incident. This is a real
// join entity (not a plain @ManyToMany) specifically so role_in_incident
// isn't silently discarded.
@Entity
@Table(name = "IncidentInmate")
public class IncidentInmate {

    @EmbeddedId
    private IncidentInmateId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("incidentId")
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("inmateId")
    @JoinColumn(name = "inmate_id")
    private Inmate inmate;

    @Column(name = "role_in_incident", length = 255)
    private String roleInIncident;

    @Column(name = "public_uuid", nullable = false, updatable = false, unique = true)
    private UUID publicUuid = UUID.randomUUID();

    protected IncidentInmate() {
    }

    // incident.getId() must already be non-null when this is called - i.e.
    // the Incident must already be saved/flushed, since incident_id is an
    // IDENTITY column only assigned on insert. IncidentService saves the
    // Incident first, then builds one of these per participant.
    public IncidentInmate(Incident incident, Inmate inmate, String roleInIncident) {
        this.incident = incident;
        this.inmate = inmate;
        this.roleInIncident = roleInIncident;
        this.id = new IncidentInmateId(incident.getId(), inmate.getId());
    }

    public IncidentInmateId getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public Inmate getInmate() {
        return inmate;
    }

    public String getRoleInIncident() {
        return roleInIncident;
    }
}
