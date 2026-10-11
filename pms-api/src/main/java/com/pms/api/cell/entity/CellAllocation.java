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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cell_allocations")
public class CellAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inmate_id", nullable = false, unique = true)
    private Inmate inmate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_id", nullable = false)
    private Cell cell;

    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt;

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

    public void moveTo(Cell newCell, Instant newAllocatedAt) {
        this.cell = newCell;
        this.allocatedAt = newAllocatedAt;
    }
}
