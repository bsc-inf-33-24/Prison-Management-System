ALTER TABLE visits
    ADD COLUMN visit_type VARCHAR(20) NOT NULL DEFAULT 'FAMILY';

ALTER TABLE visits
    ADD CONSTRAINT ck_visits_visit_type
        CHECK (visit_type IN ('FAMILY', 'LEGAL', 'HEALTH'));

CREATE TABLE family_visit_details (
    visit_id BIGINT PRIMARY KEY REFERENCES visits (visit_id) ON UPDATE CASCADE ON DELETE CASCADE,
    visitor_full_name VARCHAR(255) NOT NULL,
    items_brought VARCHAR(1000)
);

CREATE TABLE legal_visit_details (
    visit_id BIGINT PRIMARY KEY REFERENCES visits (visit_id) ON UPDATE CASCADE ON DELETE CASCADE,
    legal_case_number VARCHAR(255) NOT NULL,
    legal_representative VARCHAR(255)
);

CREATE TABLE health_visit_details (
    visit_id BIGINT PRIMARY KEY REFERENCES visits (visit_id) ON UPDATE CASCADE ON DELETE CASCADE,
    complaint TEXT NOT NULL,
    diagnosis TEXT NOT NULL,
    medications_provided TEXT,
    recommendations TEXT
);

INSERT INTO family_visit_details (visit_id, visitor_full_name, items_brought)
SELECT visit_id, visitor_full_name, items_brought
FROM visits;

INSERT INTO visits (
    public_uuid, inmate_id, visit_datetime, duration_minutes, visit_end_datetime,
    logged_by, created_at, updated_at, end_reminder_sent_at, visit_type, visitor_full_name
)
SELECT public_uuid,
       inmate_id,
       (visit_date + TIME '12:00') AT TIME ZONE 'Africa/Blantyre',
       15,
       (visit_date + TIME '12:15') AT TIME ZONE 'Africa/Blantyre',
       recorded_by_user_id,
       created_at,
       updated_at,
       created_at,
       'HEALTH',
       'Medical visit'
FROM medical_visits;

INSERT INTO health_visit_details (visit_id, complaint, diagnosis, medications_provided, recommendations)
SELECT visit.visit_id,
       medical_visit.sickness,
       medical_visit.diagnosis,
       medical_visit.medications_provided,
       medical_visit.recommendations
FROM medical_visits medical_visit
JOIN visits visit ON visit.public_uuid = medical_visit.public_uuid;

DROP INDEX IF EXISTS idx_visits_visitor_full_name;
ALTER TABLE visits
    DROP COLUMN visitor_full_name,
    DROP COLUMN items_brought;

CREATE INDEX idx_visits_visit_type_datetime ON visits (visit_type, visit_datetime DESC);
