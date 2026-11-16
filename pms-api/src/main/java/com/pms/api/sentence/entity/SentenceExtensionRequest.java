package com.pms.api.sentence.entity;

import com.pms.api.common.entity.BaseEntity;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sentence_extension_requests")
public class SentenceExtensionRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sentence_id", nullable = false)
    private InmateSentence sentence;

    @Column(name = "original_end_date", nullable = false, updatable = false)
    private LocalDate originalEndDate;

    @Column(name = "proposed_end_date", nullable = false, updatable = false)
    private LocalDate proposedEndDate;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false, updatable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id")
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SentenceExtensionStatus decision = SentenceExtensionStatus.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected SentenceExtensionRequest() { }

    public SentenceExtensionRequest(InmateSentence sentence, LocalDate originalEndDate,
                                    LocalDate proposedEndDate, String reason, User requester) {
        this.sentence = sentence;
        this.originalEndDate = originalEndDate;
        this.proposedEndDate = proposedEndDate;
        this.reason = reason;
        this.requester = requester;
    }

    public void decide(SentenceExtensionDecision decision, User reviewer, Instant decidedAt) {
        if (this.decision != SentenceExtensionStatus.PENDING) {
            throw new IllegalStateException("Only pending extension requests can be decided.");
        }
        this.decision = SentenceExtensionStatus.valueOf(decision.name());
        this.reviewer = reviewer;
        this.decidedAt = decidedAt;
    }

    public Long getId() { return id; }
    public InmateSentence getSentence() { return sentence; }
    public LocalDate getOriginalEndDate() { return originalEndDate; }
    public LocalDate getProposedEndDate() { return proposedEndDate; }
    public String getReason() { return reason; }
    public User getRequester() { return requester; }
    public User getReviewer() { return reviewer; }
    public SentenceExtensionStatus getDecision() { return decision; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getDecidedAt() { return decidedAt; }

    @jakarta.persistence.PrePersist
    private void setRequestedAt() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
