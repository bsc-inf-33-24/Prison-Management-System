package com.pms.api.inmate.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.prison.entity.Prison;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "custody_stays")
public class CustodyStay extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "prison_id", nullable = false)
    private Prison prison;
    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;
    @Column(name = "ended_at")
    private Instant endedAt;

    protected CustodyStay() { }
    public CustodyStay(Inmate inmate, Prison prison, LocalDate admissionDate) {
        this.inmate = inmate; this.prison = prison; this.admissionDate = admissionDate;
    }
    public Long getId() { return id; }
    public Inmate getInmate() { return inmate; }
    public Prison getPrison() { return prison; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public Instant getEndedAt() { return endedAt; }
    public void end(Instant at) { endedAt = at; }
    public void transferTo(Prison destination) { this.prison = destination; }
}
