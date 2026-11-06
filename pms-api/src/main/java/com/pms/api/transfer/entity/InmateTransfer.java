package com.pms.api.transfer.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.inmate.entity.Inmate;
import com.pms.api.prison.entity.Prison;
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

@Entity @Table(name = "inmate_transfers")
public class InmateTransfer extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "inmate_id", nullable = false) private Inmate inmate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_prison_id", nullable = false) private Prison sourcePrison;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "destination_prison_id", nullable = false) private Prison destinationPrison;
    @Column(name = "reason", nullable = false, length = 2000) private String reason;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) private TransferStatus status=TransferStatus.PENDING;
    @Column(name = "requested_at", nullable = false, updatable = false) private Instant requestedAt=Instant.now();
    @Column(name = "requested_by", nullable = false, length = 100) private String requestedBy;
    @Column(name = "decided_at") private Instant decidedAt;
    @Column(name = "decided_by", length = 100) private String decidedBy;
    @Column(name = "decision_note", length = 2000) private String decisionNote;
    @Column(name = "dispatched_at") private Instant dispatchedAt;
    @Column(name = "dispatched_by", length = 100) private String dispatchedBy;
    @Column(name = "received_at") private Instant receivedAt;
    @Column(name = "received_by", length = 100) private String receivedBy;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @Column(name = "cancelled_by", length = 100) private String cancelledBy;
    protected InmateTransfer() { }
    public InmateTransfer(Inmate inmate, Prison source, Prison destination, String reason, String actor) {
        this.inmate=inmate; this.sourcePrison=source; this.destinationPrison=destination; this.reason=reason; this.requestedBy=actor;
    }
    public void approve(String actor, String note) {
        require(TransferStatus.PENDING);
        Instant completedAt = Instant.now();
        status = TransferStatus.RECEIVED;
        decidedAt = completedAt;
        decidedBy = actor;
        decisionNote = note;
        dispatchedAt = completedAt;
        dispatchedBy = actor;
        receivedAt = completedAt;
        receivedBy = actor;
    }

    public void reject(String actor, String note) {
        require(TransferStatus.PENDING);
        status = TransferStatus.REJECTED;
        decidedAt = Instant.now();
        decidedBy = actor;
        decisionNote = note;
    }

    public void cancel(String actor, String note) {
        if (status!=TransferStatus.PENDING && status!=TransferStatus.APPROVED) throw new InvalidTransferStateException();
        status=TransferStatus.CANCELLED; cancelledAt=Instant.now(); cancelledBy=actor; decisionNote=note;
    }
    private void require(TransferStatus expected) { if (status!=expected) throw new InvalidTransferStateException(); }
    public Long getId() { return id; }
    public Inmate getInmate() { return inmate; }
    public Prison getSourcePrison() { return sourcePrison; }
    public Prison getDestinationPrison() { return destinationPrison; }
    public String getReason() { return reason; }
    public TransferStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getDecidedBy() { return decidedBy; }
    public String getDecisionNote() { return decisionNote; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public String getDispatchedBy() { return dispatchedBy; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getReceivedBy() { return receivedBy; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public static class InvalidTransferStateException extends RuntimeException { }
}
