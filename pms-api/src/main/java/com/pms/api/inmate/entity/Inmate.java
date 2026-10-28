package com.pms.api.inmate.entity;

import com.pms.api.cell.entity.CellAllocation;
import com.pms.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "inmates")
public class Inmate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "offence", length = 500)
    private String offence;

    @Column(name = "sentence_duration_value", nullable = false)
    private Integer sentenceDurationValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentence_duration_unit", nullable = false, length = 20)
    private SentenceDurationUnit sentenceDurationUnit;

    @Column(name = "sentence_start_date", nullable = false)
    private LocalDate sentenceStartDate;

    @Column(name = "sentence_end_date", nullable = false)
    private LocalDate sentenceEndDate;

    @OneToOne(mappedBy = "inmate", fetch = FetchType.LAZY)
    private CellAllocation cellAllocation;

    protected Inmate() {
    }

    public Inmate(String firstName, String lastName, LocalDate dateOfBirth,
                  String gender, LocalDate admissionDate,
                  String offence, LocalDate sentenceStartDate, LocalDate sentenceEndDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
        this.offence = offence;
        deriveSentenceDuration(sentenceStartDate, sentenceEndDate);
        this.sentenceStartDate = sentenceStartDate;
        this.sentenceEndDate = sentenceEndDate;
    }

    private void deriveSentenceDuration(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("A valid sentence start and end date are required.");
        }
        LocalDate exclusiveEnd = endDate.plusDays(1);
        Period period = Period.between(startDate, exclusiveEnd);
        long durationDays = ChronoUnit.DAYS.between(startDate, exclusiveEnd);
        long durationMonths = period.toTotalMonths();
        if (exclusiveEnd.isAfter(startDate.plusMonths(12))) {
            sentenceDurationValue = period.getYears();
            sentenceDurationUnit = SentenceDurationUnit.YEARS;
        } else if (durationDays > 30) {
            sentenceDurationValue = Math.toIntExact(Math.max(1, durationMonths));
            sentenceDurationUnit = SentenceDurationUnit.MONTHS;
        } else {
            sentenceDurationValue = Math.toIntExact(durationDays);
            sentenceDurationUnit = SentenceDurationUnit.DAYS;
        }
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public String getOffence() {
        return offence;
    }

    public Integer getSentenceDurationValue() {
        return sentenceDurationValue;
    }

    public SentenceDurationUnit getSentenceDurationUnit() {
        return sentenceDurationUnit;
    }

    public LocalDate getSentenceStartDate() {
        return sentenceStartDate;
    }

    public LocalDate getSentenceEndDate() {
        return sentenceEndDate;
    }

    public CellAllocation getCellAllocation() {
        return cellAllocation;
    }

    public void updateDetails(String firstName, String lastName, LocalDate dateOfBirth,
                              String gender, LocalDate admissionDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
    }
}
