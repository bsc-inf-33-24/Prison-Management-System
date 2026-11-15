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
@Table(name = "health_visit_details")
public class HealthVisitDetails {

    @Id
    @Column(name = "visit_id")
    private Long visitId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Column(name = "complaint", nullable = false, columnDefinition = "TEXT")
    private String complaint;

    @Column(name = "diagnosis", nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "medications_provided", columnDefinition = "TEXT")
    private String medicationsProvided;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    protected HealthVisitDetails() {
    }

    public HealthVisitDetails(String complaint, String diagnosis, String medicationsProvided,
                              String recommendations) {
        this.complaint = complaint;
        this.diagnosis = diagnosis;
        this.medicationsProvided = medicationsProvided;
        this.recommendations = recommendations;
    }

    void setVisit(Visit visit) {
        this.visit = visit;
    }

    public String getComplaint() {
        return complaint;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getMedicationsProvided() {
        return medicationsProvided;
    }

    public String getRecommendations() {
        return recommendations;
    }
}