package com.pms.api.inmate.entity;

import com.pms.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "inmates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inmates_inmate_number",
                columnNames = "inmate_number"
        )
)
public class Inmate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inmate_number", nullable = false, length = 30)
    private String inmateNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @Column(name = "classification", nullable = false, length = 50)
    private String classification;

    @Column(name = "current_facility", nullable = false, length = 100)
    private String currentFacility;

    protected Inmate() {
    }

    public Inmate(String inmateNumber, String firstName, String lastName, LocalDate dateOfBirth,
                  String gender, LocalDate admissionDate, String classification, String currentFacility) {
        this.inmateNumber = inmateNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
        this.classification = classification;
        this.currentFacility = currentFacility;
    }

    public Long getId() {
        return id;
    }

    public String getInmateNumber() {
        return inmateNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public String getClassification() {
        return classification;
    }

    public String getCurrentFacility() {
        return currentFacility;
    }

    public void updateDetails(String inmateNumber, String firstName, String lastName, LocalDate dateOfBirth,
                              String gender, LocalDate admissionDate, String classification) {
        this.inmateNumber = inmateNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
        this.classification = classification;
    }
}
