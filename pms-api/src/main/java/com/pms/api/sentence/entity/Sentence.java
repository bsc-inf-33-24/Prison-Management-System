package com.pms.api.sentence.entity;

import com.pms.api.cases.entity.Case;
import com.pms.api.common.entity.BaseEntity;
import com.pms.api.inmate.entity.Inmate;
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
import java.util.Set;

@Entity
@Table(name = "sentences")
public class Sentence extends BaseEntity {

    public static final Set<String> ALLOWED_STATUSES = Set.of("active", "completed", "commuted");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sentence_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    protected Sentence() {
    }

    private Sentence(Case caseEntity, Inmate inmate, Integer duration, LocalDate startDate, LocalDate releaseDate, String status) {
        this.caseEntity = caseEntity;
        this.inmate = inmate;
        this.duration = duration;
        this.startDate = startDate;
        this.releaseDate = releaseDate;
        this.status = normalizeStatus(status);
    }

    public static Sentence create(Case caseEntity, Inmate inmate, Integer duration, LocalDate startDate,
                                  LocalDate releaseDate, String status) {
        return new Sentence(caseEntity, inmate, duration, startDate, releaseDate, status);
    }

    public Long getId() {
        return id;
    }

    public Case getCase() {
        return caseEntity;
    }

    public Long getCaseId() {
        return caseEntity == null ? null : caseEntity.getId();
    }

    public Inmate getInmate() {
        return inmate;
    }

    public Long getInmateId() {
        return inmate == null ? null : inmate.getId();
    }

    public Integer getDuration() {
        return duration;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void updateDetails(Case caseEntity, Inmate inmate, Integer duration, LocalDate startDate,
                             LocalDate releaseDate, String status) {
        this.caseEntity = caseEntity;
        this.inmate = inmate;
        this.duration = duration;
        this.startDate = startDate;
        this.releaseDate = releaseDate;
        this.status = normalizeStatus(status);
    }

    public static String normalizeStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Sentence status is required.");
        }
        String normalized = status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Status must be one of: active, completed, commuted");
        }
        return normalized;
    }
}
