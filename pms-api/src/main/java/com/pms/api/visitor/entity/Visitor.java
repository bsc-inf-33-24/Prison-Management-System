package com.pms.api.visitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "visitors",
        uniqueConstraints = @UniqueConstraint(name = "uk_visitors_id_number", columnNames = "id_number")
)
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visitor_id")
    private Long id;

    @Column(name = "public_uuid", nullable = false, updatable = false, unique = true)
    private UUID publicUuid = UUID.randomUUID();

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "id_number", nullable = false, length = 255)
    private String idNumber;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "relationship_to_inmate", nullable = false, length = 255)
    private String relationshipToInmate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Visitor() {
    }

    public Visitor(String fullName, String idNumber, String phone, String relationshipToInmate) {
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.phone = phone;
        this.relationshipToInmate = relationshipToInmate;
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicUuid() {
        return publicUuid;
    }

    public String getFullName() {
        return fullName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getPhone() {
        return phone;
    }

    public String getRelationshipToInmate() {
        return relationshipToInmate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
