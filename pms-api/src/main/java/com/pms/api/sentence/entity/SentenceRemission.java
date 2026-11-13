package com.pms.api.sentence.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sentence_remissions")
public class SentenceRemission extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sentence_id", nullable = false)
    private InmateSentence sentence;
    @Column(name = "remission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal remissionPercentage;
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    protected SentenceRemission() { }
    public SentenceRemission(InmateSentence sentence, BigDecimal remissionPercentage,
                             LocalDate effectiveDate, String reason, User approvedBy) {
        this.sentence = sentence; this.remissionPercentage = remissionPercentage;
        this.effectiveDate = effectiveDate; this.reason = reason; this.approvedBy = approvedBy;
    }
    public Long getId() { return id; }
    public InmateSentence getSentence() { return sentence; }
    public BigDecimal getRemissionPercentage() { return remissionPercentage; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getReason() { return reason; }
    public User getApprovedBy() { return approvedBy; }
}
