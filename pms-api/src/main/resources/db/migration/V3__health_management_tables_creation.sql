
create table HealthRecords(
    health_record_id BIGSERIAL PRIMARY KEY,
    inmate_id BIGINT REFERENCES Inmate(inmate_id) ON UPDATE CASCADE ON DELETE SET NULL,
    screening_date DATE,
    screening_results VARCHAR(255),
    blood_group VARCHAR(10),
    created_by VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    status VARCHAR(50) CHECK (status IN ('ongoing','critical','recovered')),
);

create table MedicalCondition(
    condition_id BIGSERIAL PRIMARY KEY,
    health_record_id BIGINT REFERENCES HealthRecords(health_record_id) ON UPDATE CASCADE ON DELETE SET NULL,
    condition_type VARCHAR(255),
    condition_name VARCHAR(255),
    severity VARCHAR(255),
    date_recorded DATE,
    status VARCHAR(50) CHECK (status IN ('active','resolved')),
    recorded_by VARCHAR(255)
);

create table Consultation(
    consultation_id BIGSERIAL PRIMARY KEY,
    health_record_id BIGINT REFERENCES HealthRecords(health_record_id) ON UPDATE CASCADE ON DELETE SET NULL,
    consultation_date DATE,
    complaint TEXT,
    diagnosis TEXT,
    treatment_provided TEXT,
    consulted_by VARCHAR(255)
);

create table Medication(
    medication_id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT REFERENCES Consultation(condition_id) ON UPDATE CASCADE ON DELETE SET NULL,
    drug_name VARCHAR(255),
    dosage VARCHAR(255),
    frequency VARCHAR(255),
    start_date DATE,
    end_date DATE,
    status ,
    prescribed_by VARCHAR(255)
);

create table MedicationAdministration(
    administration_id BIGSERIAL PRIMARY KEY,
    medication_id BIGINT REFERENCES Medication(medication_id) ON UPDATE CASCADE ON DELETE SET NULL,
    administrated_datetime DATE,
    dose_given VARCHAR(255),
    administrated_by VARCHAR(255)
);

create table MedicalAppointment(
    appointment_id BIGSERIAL PRIMARY KEY,
    health_record_id BIGINT REFERENCES HealthRecords(health_record_id) ON UPDATE CASCADE ON DELETE SET NULL,
    appointment_datetime DATE,
    purpose VARCHAR(255),
    appointment_type VARCHAR(100) CHECK (appointment_type IN ('internal','external')),
    external_facility VARCHAR(255),
    status VARCHAR(50) CHECK (status IN ('scheduled','completed','cancelled')),
    scheduled_by VARCHAR(255)
);