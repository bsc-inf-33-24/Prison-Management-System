package com.pms.api.inmate.entity;

import com.pms.api.cell.entity.CellAllocation;
import com.pms.api.common.entity.BaseEntity;
import com.pms.api.prison.entity.Prison;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "inmates")
public class Inmate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "inmate_type", nullable = false, length = 20)
    private InmateType inmateType;

    @Column(name = "bio", nullable = false, columnDefinition = "TEXT")
    private String bio;

    @Column(name = "medical_history", nullable = false, columnDefinition = "TEXT")
    private String medicalHistory;

    @Column(name = "expected_court_date")
    private LocalDate expectedCourtDate;

    @Column(name = "next_of_kin_name", length = 200)
    private String nextOfKinName;
    @Column(name = "next_of_kin_relationship", length = 100)
    private String nextOfKinRelationship;
    @Column(name = "next_of_kin_phone", length = 40)
    private String nextOfKinPhone;
    @Column(name = "next_of_kin_email", length = 254)
    private String nextOfKinEmail;
    @Column(name = "contact_phone", length = 40)
    private String contactPhone;
    @Column(name = "contact_email", length = 254)
    private String contactEmail;

    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate;

    @OneToMany(mappedBy = "inmate", fetch = FetchType.LAZY)
    private java.util.List<CellAllocation> cellAllocations = new java.util.ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_prison_id")
    private Prison currentPrison;

    protected Inmate() {
    }

    public Inmate(String firstName, String lastName, LocalDate dateOfBirth, String gender,
                  LocalDate admissionDate, InmateType inmateType, String bio, String medicalHistory,
                  LocalDate expectedCourtDate, String nextOfKinName, String nextOfKinRelationship,
                  String nextOfKinPhone, String nextOfKinEmail, String contactPhone, String contactEmail) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
        this.inmateType = inmateType;
        this.bio = bio;
        this.medicalHistory = medicalHistory;
        this.expectedCourtDate = expectedCourtDate;
        this.nextOfKinName = nextOfKinName;
        this.nextOfKinRelationship = nextOfKinRelationship;
        this.nextOfKinPhone = nextOfKinPhone;
        this.nextOfKinEmail = nextOfKinEmail;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
    }

    /**
     * Compatibility constructor for existing service and integration-test fixtures.
     * Sentence values are intentionally not stored on an inmate; create a sentence
     * through the sentence module instead.
     */
    @Deprecated
    public Inmate(String firstName, String lastName, LocalDate dateOfBirth, String gender,
                  LocalDate admissionDate, String legacyOffence, LocalDate legacySentenceStartDate,
                  LocalDate legacySentenceEndDate) {
        this(firstName, lastName, dateOfBirth, gender, admissionDate, InmateType.SENTENCED,
                "Not provided", "Not recorded", null, null, null, null, null, null, null);
    }

    public Long getId() {
        return id;
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

    public CellAllocation getCellAllocation() {
        return cellAllocations.stream().filter(CellAllocation::isActive).findFirst().orElse(null);
    }

    public Prison getCurrentPrison() { return currentPrison; }

    public InmateType getInmateType() { return inmateType; }
    public String getBio() { return bio; }
    public String getMedicalHistory() { return medicalHistory; }
    public LocalDate getExpectedCourtDate() { return expectedCourtDate; }
    public String getNextOfKinName() { return nextOfKinName; }
    public String getNextOfKinRelationship() { return nextOfKinRelationship; }
    public String getNextOfKinPhone() { return nextOfKinPhone; }
    public String getNextOfKinEmail() { return nextOfKinEmail; }
    public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; }

    public void assignPrison(Prison prison) { this.currentPrison = prison; }

    public void updateDetails(String firstName, String lastName, LocalDate dateOfBirth,
                              String gender, LocalDate admissionDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
    }
}
