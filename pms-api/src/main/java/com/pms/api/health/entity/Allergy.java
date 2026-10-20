package com.pms.api.health.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.inmate.entity.Inmate;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "allergies")
public class Allergy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allergy_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_inmate_id", nullable = false)
    private Inmate inmate;

    @Column(name = "allergen", nullable = false, length = 255)
    private String allergen;

    @Column(name = "reaction")
    private String reaction;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    protected Allergy() {}

    public Allergy(Inmate inmate, String allergen, String reaction,
                   String severity, LocalDate recordedDate, String status) {
        this.inmate = inmate;
        this.allergen = allergen;
        this.reaction = reaction;
        this.severity = severity;
        this.recordedDate = recordedDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public Inmate getInmate() { return inmate; }
    public String getAllergen() { return allergen; }
    public String getReaction() { return reaction; }
    public String getSeverity() { return severity; }
    public LocalDate getRecordedDate() { return recordedDate; }
    public String getStatus() { return status; }
}