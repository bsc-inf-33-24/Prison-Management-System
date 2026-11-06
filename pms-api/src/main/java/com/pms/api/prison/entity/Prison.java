package com.pms.api.prison.entity;

import com.pms.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name = "prisons")
public class Prison extends BaseEntity {
    @jakarta.persistence.Id @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column(name = "code", nullable = false, unique = true, length = 32) private String code;
    @Column(name = "name", nullable = false, length = 160) private String name;
    @Column(name = "location", length = 255) private String location;
    @Column(name = "active", nullable = false) private boolean active = true;
    protected Prison() { }
    public Prison(String code, String name, String location) { this.code=code; this.name=name; this.location=location; }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public boolean isActive() { return active; }
    public void update(String name, String location) { this.name=name; this.location=location; }
    public void setActive(boolean active) { this.active=active; }
}
