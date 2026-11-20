package com.pms.api.sentence.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.inmate.entity.SentenceType;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "inmate_sentences")
public class InmateSentence extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;
    @Column(nullable = false, length = 500)
    private String offence;
    @Enumerated(EnumType.STRING) @Column(name = "sentence_type", nullable = false, length = 20)
    private SentenceType sentenceType;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SentenceStatus status = SentenceStatus.ACTIVE;

    protected InmateSentence() { }
    public InmateSentence(Inmate inmate, String offence, SentenceType sentenceType, LocalDate startDate,
                          LocalDate endDate) {
        this.inmate = inmate; this.offence = offence; this.sentenceType = sentenceType;
        this.startDate = startDate; this.endDate = endDate;
        if (sentenceType == SentenceType.FIXED_TERM && (endDate == null || endDate.isBefore(startDate)))
            throw new IllegalArgumentException("A fixed-term sentence requires a valid end date.");
        if (sentenceType != SentenceType.FIXED_TERM && endDate != null)
            throw new IllegalArgumentException("Life and undetermined sentences must not have an end date.");
    }
    public Long getId() { return id; }
    public Inmate getInmate() { return inmate; }
    public String getOffence() { return offence; }
    public SentenceType getSentenceType() { return sentenceType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public SentenceStatus getStatus() { return status; }
    public void changeStatus(SentenceStatus status) { this.status = status; }
}
