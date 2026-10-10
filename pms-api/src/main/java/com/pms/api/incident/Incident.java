package com.pms.api.incident;

import com.pms.api.inmate.Inmate;
import com.pms.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "incidents")
@Getter
public class Incident {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "incident_date", nullable = false)
    private OffsetDateTime incidentDate;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Setter
    @Column(name = "outcome", length = 2000)
    private String outcome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "logged_by", nullable = false, updatable = false)
    private User loggedBy;

    @ManyToMany
    @JoinTable(
            name = "incident_inmates",
            joinColumns = @JoinColumn(name = "incident_id"),
            inverseJoinColumns = @JoinColumn(name = "inmate_id")
    )
    private Set<Inmate> inmates = new HashSet<>();

    protected Incident() {
    }

    private Incident(OffsetDateTime incidentDate, String description, User loggedBy, Set<Inmate> inmates) {
        this.incidentDate = incidentDate;
        this.description = description;
        this.loggedBy = loggedBy;
        this.inmates = inmates;
        this.createdAt = OffsetDateTime.now();
    }

    public static Incident logNew(OffsetDateTime incidentDate, String description, User loggedBy, Set<Inmate> inmates) {
        return new Incident(incidentDate, description, loggedBy, inmates);
    }
}
