package com.pms.api.prison.entity;

import com.pms.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "blocks", uniqueConstraints = @UniqueConstraint(name = "uk_blocks_prison_name", columnNames = {"prison_id", "name"}))
public class Block extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "prison_id", nullable = false) private Prison prison;
    @Column(name = "name", nullable = false, length = 50) private String name;
    @OneToMany(mappedBy = "block", fetch = FetchType.LAZY) private List<com.pms.api.cell.entity.Cell> cells = new ArrayList<>();
    protected Block() { }
    public Block(Prison prison, String name) { this.prison=prison; this.name=name; }
    public void rename(String name) { this.name=name; }
    public Long getId() { return id; }
    public Prison getPrison() { return prison; }
    public String getName() { return name; }
    public List<com.pms.api.cell.entity.Cell> getCells() { return cells; }
}
