package com.pms.api.cases.entity;

import com.pms.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "crimes")
public class Crime extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offence_name", nullable = false, length = 150)
    private String offenceName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    protected Crime() {
    }

    public Crime(String offenceName, String description, String category) {
        this.offenceName = offenceName;
        this.description = description;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getOffenceName() {
        return offenceName;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public void update(String offenceName, String description, String category) {
        this.offenceName = offenceName;
        this.description = description;
        this.category = category;
    }
}
