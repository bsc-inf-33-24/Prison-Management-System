package com.pms.api.cell.entity;

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
import java.time.Instant;

@Entity
@Table(name = "cell_allocations")
public class CellAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmate_id", nullable = false)
    private Inmate inmate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_id", nullable = false)
    private Cell cell;

    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    protected CellAllocation() {
    }

    public CellAllocation(Inmate inmate, Cell cell, Instant allocatedAt) {
        this.inmate = inmate;
        this.cell = cell;
        this.allocatedAt = allocatedAt;
    }

    public Long getId() {
        return id;
    }

    public Inmate getInmate() {
        return inmate;
    }

    public Cell getCell() {
        return cell;
    }

    public Instant getAllocatedAt() {
        return allocatedAt;
    }

    public Instant getReleasedAt() { return releasedAt; }

    public boolean isActive() { return releasedAt == null; }

    public void release(Instant releasedAt) { this.releasedAt = releasedAt; }
}
