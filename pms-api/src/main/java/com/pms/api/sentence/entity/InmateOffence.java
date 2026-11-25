package com.pms.api.sentence.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.inmate.entity.Inmate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inmate_offences")
public class InmateOffence extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;

    @Column(name = "offence", nullable = false, length = 500)
    private String offence;

    protected InmateOffence() { }

    public InmateOffence(Inmate inmate, String offence) {
        this.inmate = inmate;
        this.offence = offence.trim();
    }

    public Long getId() { return id; }
    public Inmate getInmate() { return inmate; }
    public String getOffence() { return offence; }
    public void updateOffence(String offence) { this.offence = offence.trim(); }
}
