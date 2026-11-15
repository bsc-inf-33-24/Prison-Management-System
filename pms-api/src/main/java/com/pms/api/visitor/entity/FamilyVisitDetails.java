package com.pms.api.visitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "family_visit_details")
public class FamilyVisitDetails {

    @Id
    @Column(name = "visit_id")
    private Long visitId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "visit_id")
    private Visit visit;

    @Column(name = "visitor_full_name", nullable = false, length = 255)
    private String visitorFullName;

    @Column(name = "items_brought", length = 1000)
    private String itemsBrought;

    protected FamilyVisitDetails() {
    }

    public FamilyVisitDetails(String visitorFullName, String itemsBrought) {
        this.visitorFullName = visitorFullName;
        this.itemsBrought = itemsBrought;
    }

    void setVisit(Visit visit) {
        this.visit = visit;
    }

    public String getVisitorFullName() {
        return visitorFullName;
    }

    public String getItemsBrought() {
        return itemsBrought;
    }
}