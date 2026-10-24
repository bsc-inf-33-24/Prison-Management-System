package com.pms.api.health.entity;

import com.pms.api.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "medicalcondition")
public class MedicalCondition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id")
    private HealthRecord healthRecord;

    @Column(name = "condition_type", length = 255)
    private String conditionType;

    @Column(name = "condition_name", length = 255)
    private String conditionName;

    @Column(name = "severity", length = 255)
    private String severity;

    @Column(name = "date_recorded")
    private LocalDate dateRecorded;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "recorded_by", length = 255)
    private String recordedBy;

    protected MedicalCondition() {
    }

    public MedicalCondition(
            HealthRecord healthRecord,
            String conditionType,
            String conditionName,
            String severity,
            LocalDate dateRecorded,
            String status,
            String recordedBy) {
        this.healthRecord = healthRecord;
        this.conditionType = conditionType;
        this.conditionName = conditionName;
        this.severity = severity;
        this.dateRecorded = dateRecorded;
        this.status = status;
        this.recordedBy = recordedBy;
    }

    public Long getId() { return id; }
    public HealthRecord getHealthRecord() { return healthRecord; }
    public String getConditionType() { return conditionType; }
    public String getConditionName() { return conditionName; }
    public String getSeverity() { return severity; }
    public LocalDate getDateRecorded() { return dateRecorded; }
    public String getStatus() { return status; }
    public String getRecordedBy() { return recordedBy; }
}
