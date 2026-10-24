package com.pms.api.health.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.inmate.entity.Inmate;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "healthrecords")
public class HealthRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "health_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_inmate_id")
    private Inmate inmate;

    @Column(name = "screening_date")
    private LocalDate screeningDate;

    @Column(name = "screening_results", length = 255)
    private String screeningResults;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "status", length = 50)
    private String status;

    protected HealthRecord() {}

    public HealthRecord(Inmate inmate, LocalDate screeningDate,
                        String screeningResults, String bloodGroup, String status) {
        this.inmate = inmate;
        this.screeningDate = screeningDate;
        this.screeningResults = screeningResults;
        this.bloodGroup = bloodGroup;
        this.status = status;
    }

    public Long getId() { return id; }
    public Inmate getInmate() { return inmate; }
    public LocalDate getScreeningDate() { return screeningDate; }
    public String getScreeningResults() { return screeningResults; }
    public String getBloodGroup() { return bloodGroup; }
    public String getStatus() { return status; }
}
