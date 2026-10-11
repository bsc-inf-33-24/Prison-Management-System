package com.pms.api.cell.entity;

import com.pms.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "cells",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cells_block_number",
                columnNames = {"block_name", "cell_number"}
        )
)
public class Cell extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "block_name", nullable = false, length = 50)
    private String blockName;

    @Column(name = "cell_number", nullable = false)
    private Integer cellNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "classification", nullable = false, length = 50)
    private String classification;

    protected Cell() {
    }

    public Cell(String blockName, Integer cellNumber, Integer capacity, String classification) {
        this.blockName = blockName;
        this.cellNumber = cellNumber;
        this.capacity = capacity;
        this.classification = classification;
    }

    public Long getId() {
        return id;
    }

    public String getBlockName() {
        return blockName;
    }

    public Integer getCellNumber() {
        return cellNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getClassification() {
        return classification;
    }

    public String getName() {
        return blockName + "-" + cellNumber;
    }
}
