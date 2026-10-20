package com.pms.api.health.entity;

import com.pms.api.common.entity.BaseEntity;
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
@Table(name = "medicalappointment")
public class MedicalAppointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id")
    private HealthRecord healthRecord;

    @Column(name = "appointment_datetime")
    private LocalDate appointmentDate;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "appointment_type")
    private String appointmentType;

    @Column(name = "external_facility")
    private String externalFacility;

    @Column(name = "status")
    private String status;

    @Column(name = "scheduled_by")
    private String scheduledBy;

    protected MedicalAppointment() {
    }

    public MedicalAppointment(
            HealthRecord healthRecord,
            LocalDate appointmentDate,
            String purpose,
            String appointmentType,
            String externalFacility,
            String status,
            String scheduledBy) {
        this.healthRecord = healthRecord;
        this.appointmentDate = appointmentDate;
        this.purpose = purpose;
        this.appointmentType = appointmentType;
        this.externalFacility = externalFacility;
        this.status = status;
        this.scheduledBy = scheduledBy;
    }

    public Long getId() {
        return id;
    }

    public HealthRecord getHealthRecord() {
        return healthRecord;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getAppointmentType() {
        return appointmentType;
    }

    public String getExternalFacility() {
        return externalFacility;
    }

    public String getStatus() {
        return status;
    }

    public String getScheduledBy() {
        return scheduledBy;
    }
}