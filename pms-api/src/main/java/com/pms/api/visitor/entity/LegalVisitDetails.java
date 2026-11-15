package com.pms.api.visitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "legal_visit_details")
public class LegalVisitDetails {

    @Id
    @Column(name = "visit_id")
    private Long visitId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Column(name = "legal_case_number", nullable = false, length = 255)
    private String legalCaseNumber;

    @Column(name = "legal_representative", length = 255)
    private String legalRepresentative;

    protected LegalVisitDetails() {
    }

    public LegalVisitDetails(String legalCaseNumber, String legalRepresentative) {
        this.legalCaseNumber = legalCaseNumber;
        this.legalRepresentative = legalRepresentative;
    }

    void setVisit(Visit visit) {
        this.visit = visit;
    }

    public String getLegalCaseNumber() {
        return legalCaseNumber;
    }

    public String getLegalRepresentative() {
        return legalRepresentative;
    }
}