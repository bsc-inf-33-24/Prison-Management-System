package com.pms.api.rehabilitation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;


@Entity
@Table(name = "rehabprogram")
public class RehabilitationProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "program_id")
    private Long id;

    @Column(name = "public_uuid", nullable = false, updatable = false, unique = true)
    private UUID publicUuid = UUID.randomUUID();

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "instructor", nullable = false, length = 255)
    private String instructor;

    @Column(name = "schedule", length = 255)
    private String schedule;

    @Column(name = "status", length = 255)
    private String status;

    protected RehabilitationProgram() {
    }

    public RehabilitationProgram(String name, String description, String instructor,
                                  String schedule, String status) {
        this.name = name;
        this.description = description;
        this.instructor = instructor;
        this.schedule = schedule;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicUuid() {
        return publicUuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInstructor() {
        return instructor;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getStatus() {
        return status;
    }

    public void updateDetails(String name, String description, String instructor, String schedule) {
        this.name = name;
        this.description = description;
        this.instructor = instructor;
        this.schedule = schedule;
    }

}
