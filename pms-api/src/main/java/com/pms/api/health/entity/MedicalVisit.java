package com.pms.api.health.entity;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "medical_visits")
public class MedicalVisit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false, updatable = false)
    private Inmate inmate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false, updatable = false)
    private User recordedBy;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "sickness", nullable = false, columnDefinition = "TEXT")
    private String sickness;

    @Column(name = "diagnosis", nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "medications_provided", columnDefinition = "TEXT")
    private String medicationsProvided;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    protected MedicalVisit() {
    }

    public MedicalVisit(Inmate inmate, User recordedBy, LocalDate visitDate, String sickness,
                        String diagnosis, String medicationsProvided, String recommendations) {
        this.inmate = inmate;
        this.recordedBy = recordedBy;
        this.visitDate = visitDate;
        this.sickness = sickness;
        this.diagnosis = diagnosis;
        this.medicationsProvided = medicationsProvided;
        this.recommendations = recommendations;
    }

    public Long getId() {
        return id;
    }

    public Inmate getInmate() {
        return inmate;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public String getSickness() {
        return sickness;
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
