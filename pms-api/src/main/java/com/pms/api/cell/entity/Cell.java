package com.pms.api.cell.entity;

import com.pms.api.common.entity.BaseEntity;
import com.pms.api.prison.entity.Block;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "cells")
public class Cell extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_name", nullable = false, length = 50)
    private String blockName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @Column(name = "cell_number", nullable = false)
    private Integer cellNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    protected Cell() {
    }

    public Cell(String blockName, Integer cellNumber, Integer capacity) {
        this.blockName = blockName;
        this.cellNumber = cellNumber;
        this.capacity = capacity;
    }

    public Cell(Block block, Integer cellNumber, Integer capacity) {
        this.block = block;
        this.blockName = block.getName();
        this.cellNumber = cellNumber;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public String getBlockName() {
        return block == null ? blockName : block.getName();
    }

    public Block getBlock() { return block; }
    public void assignBlock(Block block) { this.block=block; this.blockName=block.getName(); }

    public Integer getCellNumber() {
        return cellNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getName() {
        return getBlockName() + "-" + cellNumber;
    }
}
