package com.pms.api.inmate.entity;

import com.pms.api.cell.entity.CellAllocation;
import com.pms.api.common.entity.BaseEntity;
import com.pms.api.prison.entity.Prison;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "sentence_type", nullable = false, length = 20)
    private SentenceType sentenceType = SentenceType.FIXED_TERM;

    @Column(name = "remission_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal remissionPercentage;

    @Column(name = "projected_release_date")
    private LocalDate projectedReleaseDate;

    @Column(name = "release_reminder_sent_at")
    private java.time.Instant releaseReminderSentAt;

    @OneToMany(mappedBy = "inmate", fetch = FetchType.LAZY)
    private java.util.List<CellAllocation> cellAllocations = new java.util.ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_prison_id")
    private Prison currentPrison;

    protected Inmate() {
    }

    public Inmate(String firstName, String lastName, LocalDate dateOfBirth,
                  String gender, LocalDate admissionDate,
                  String offence, LocalDate sentenceStartDate, LocalDate sentenceEndDate) {
        this(firstName, lastName, dateOfBirth, gender, admissionDate, offence, sentenceStartDate,
                SentenceType.FIXED_TERM, sentenceEndDate, null);
    }

    public Inmate(String firstName, String lastName, LocalDate dateOfBirth, String gender, LocalDate admissionDate,
                  String offence, LocalDate sentenceStartDate, SentenceType sentenceType,
                  LocalDate sentenceEndDate, java.math.BigDecimal remissionPercentage) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
        this.offence = offence;
        this.sentenceType = sentenceType;
        this.sentenceStartDate = sentenceStartDate;
        this.sentenceEndDate = sentenceEndDate;
        this.remissionPercentage = remissionPercentage;
        if (sentenceType == SentenceType.FIXED_TERM) {
            deriveSentenceDuration(sentenceStartDate, sentenceEndDate);
            long days = ChronoUnit.DAYS.between(sentenceStartDate, sentenceEndDate) + 1;
            long reducedDays = java.math.BigDecimal.valueOf(days)
                    .multiply(java.math.BigDecimal.ONE.subtract((remissionPercentage == null
                            ? java.math.BigDecimal.ZERO : remissionPercentage).movePointLeft(2)))
                    .setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
            this.projectedReleaseDate = sentenceStartDate.plusDays(Math.max(0, reducedDays - 1));
        } else if (sentenceEndDate != null || remissionPercentage != null) {
            throw new IllegalArgumentException("Life and undetermined sentences cannot have an end date or remission percentage.");
        }
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
    public SentenceType getSentenceType() { return sentenceType; }
    public java.math.BigDecimal getRemissionPercentage() { return remissionPercentage; }
    public LocalDate getProjectedReleaseDate() { return projectedReleaseDate; }
    public java.time.Instant getReleaseReminderSentAt() { return releaseReminderSentAt; }

    public CellAllocation getCellAllocation() {
        return cellAllocations.stream().filter(CellAllocation::isActive).findFirst().orElse(null);
    }

    public Prison getCurrentPrison() { return currentPrison; }

    public void assignPrison(Prison prison) { this.currentPrison = prison; }

    public void updateDetails(String firstName, String lastName, LocalDate dateOfBirth,
                              String gender, LocalDate admissionDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
    }
}
