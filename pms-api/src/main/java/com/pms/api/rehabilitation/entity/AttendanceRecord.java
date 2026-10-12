package com.pms.api.rehabilitation.entity;

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
@Table(name = "attendancerecord")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @Column(name = "session_date")
    private LocalDate sessionDate;

    /** Stored as Y/N by the current V7 migration. */
    @Column(name = "present", nullable = false, length = 10)
    private String present;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "recorded_by", nullable = false, length = 255)
    private String recordedBy;

    protected AttendanceRecord() {
    }

    public AttendanceRecord(Enrollment enrollment, LocalDate sessionDate,
                            String present, String remarks, String recordedBy) {
        this.enrollment = enrollment;
        this.sessionDate = sessionDate;
        this.present = present;
        this.remarks = remarks;
        this.recordedBy = recordedBy;
    }

    public Long getId() {
        return id;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public String getPresent() {
        return present;
    }

    public String getRemarks() {
        return remarks;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void updateAttendance(String present, String remarks) {
        this.present = present;
        this.remarks = remarks;
    }
}
