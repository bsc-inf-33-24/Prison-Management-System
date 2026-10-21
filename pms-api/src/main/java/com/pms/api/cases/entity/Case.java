package com.pms.api.cases.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.sentence.entity.Sentence;
import com.pms.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(
        name = "cases",
        uniqueConstraints = @UniqueConstraint(name = "uk_cases_case_number", columnNames = "case_number")
)
public class Case extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;

    @Column(name = "case_number", nullable = false, length = 80, unique = true)
    private String caseNumber;

    @Column(name = "court", nullable = false, length = 150)
    private String court;

    @Column(name = "crimes", nullable = false, columnDefinition = "text")
    private String crimes;

    @Column(name = "case_start_date", nullable = false)
    private LocalDate caseStartDate;

    @Column(name = "case_end_date", nullable = false)
    private LocalDate caseEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CaseStatus status = CaseStatus.ONGOING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    protected Case() {
    }

    public Case(Inmate inmate, String caseNumber, String court, String crimes, LocalDate caseStartDate,
                LocalDate caseEndDate, User createdBy) {
        this.inmate = inmate;
        this.caseNumber = caseNumber;
        this.court = court;
        this.crimes = crimes;
        this.caseStartDate = caseStartDate;
        this.caseEndDate = caseEndDate;
        this.createdBy = createdBy;
        this.status = CaseStatus.ONGOING;
    }

    public Long getId() {
        return id;
    }

    public Inmate getInmate() {
        return inmate;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public String getCourt() {
        return court;
    }

    public String getCrimes() {
        return crimes;
    }

    public LocalDate getCaseStartDate() {
        return caseStartDate;
    }

    public LocalDate getCaseEndDate() {
        return caseEndDate;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void updateDetails(String caseNumber, String court, String crimes, LocalDate caseStartDate, LocalDate caseEndDate) {
        this.caseNumber = caseNumber;
        this.court = court;
        this.crimes = crimes;
        this.caseStartDate = caseStartDate;
        this.caseEndDate = caseEndDate;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public void replaceCrimes(String crimes) {
        this.crimes = crimes;
    }
}
