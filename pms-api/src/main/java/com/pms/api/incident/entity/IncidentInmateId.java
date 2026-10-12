package com.pms.api.incident.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

// The composite primary key V5 defines for IncidentInmate:
// PRIMARY KEY (incident_id, inmate_id). No surrogate id column exists in
// the migration, so none is added here.
@Embeddable
public class IncidentInmateId implements Serializable {

    @Column(name = "incident_id")
    private Long incidentId;

    @Column(name = "inmate_id")
    private Long inmateId;

    protected IncidentInmateId() {
    }

    public IncidentInmateId(Long incidentId, Long inmateId) {
        this.incidentId = incidentId;
        this.inmateId = inmateId;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public Long getInmateId() {
        return inmateId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IncidentInmateId that)) {
            return false;
        }
        return Objects.equals(incidentId, that.incidentId) && Objects.equals(inmateId, that.inmateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incidentId, inmateId);
    }
}
