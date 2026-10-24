package com.pms.api.health.entity;

import com.pms.api.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "consultation")
public class Consultation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consultation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id")
    private HealthRecord healthRecord;

    @Column(name = "consultation_date")
    private LocalDate consultationDate;

    @Column(name = "complaint", columnDefinition = "TEXT")
    private String complaint;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "treatment_provided", columnDefinition = "TEXT")
    private String treatmentProvided;

    @Column(name = "consulted_by", length = 255)
    private String consultedBy;

    protected Consultation() {
    }

    public Consultation(
            HealthRecord healthRecord,
            LocalDate consultationDate,
            String complaint,
            String diagnosis,
            String treatmentProvided,
            String consultedBy) {
        this.healthRecord = healthRecord;
        this.consultationDate = consultationDate;
        this.complaint = complaint;
        this.diagnosis = diagnosis;
        this.treatmentProvided = treatmentProvided;
        this.consultedBy = consultedBy;
    }

    public Long getId() {
        return id;
    }

    public HealthRecord getHealthRecord() {
        return healthRecord;
    }

    public LocalDate getConsultationDate() {
        return consultationDate;
    }

    public String getComplaint() {
        return complaint;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getTreatmentProvided() {
        return treatmentProvided;
    }

    public String getConsultedBy() {
        return consultedBy;
    }
}