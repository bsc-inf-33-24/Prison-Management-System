-- Demo data for Maula and Chichiri. Safe to run repeatedly; uses stable codes/numbers.
-- Run against a database after all Flyway migrations have completed.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO prisons (public_uuid, code, name, location)
VALUES
    ('10000000-0000-4000-8000-000000000001', 'MAULA', 'Maula Prison', 'Lilongwe'),
    ('10000000-0000-4000-8000-000000000002', 'CHICHIRI', 'Chichiri Prison', 'Blantyre')
ON CONFLICT (code) DO NOTHING;

INSERT INTO blocks (public_uuid, prison_id, name)
SELECT gen_random_uuid(), p.id, b.name
FROM prisons p CROSS JOIN (VALUES ('A'), ('B')) AS b(name)
WHERE p.code IN ('MAULA', 'CHICHIRI')
ON CONFLICT (prison_id, name) DO NOTHING;

INSERT INTO cells (block_name, cell_number, capacity, classification, block_id)
SELECT b.name, c.cell_number, 4, 'Medium', b.id
FROM blocks b JOIN prisons p ON p.id = b.prison_id
CROSS JOIN (VALUES (1), (2)) AS c(cell_number)
WHERE p.code IN ('MAULA', 'CHICHIRI')
ON CONFLICT (block_id, cell_number) WHERE block_id IS NOT NULL DO NOTHING;

-- Demo-only shared password for all generated accounts; see scripts/DEMO-CREDENTIALS.txt.
INSERT INTO users (public_uuid, full_name, username, password_hash, role, prison_id)
SELECT gen_random_uuid(), u.full_name, u.username,
       crypt('DemoPMS2026!', gen_salt('bf')), u.role, p.id
FROM prisons p
CROSS JOIN (VALUES
    ('Maula Records Officer', 'maula.records', 'Records Officer'),
    ('Maula Medical Officer', 'maula.medical', 'Medical Officer'),
    ('Maula Duty Officer', 'maula.duty', 'Duty Officer'),
    ('Maula Visitor Officer', 'maula.visitor', 'Visitor Officer'),
    ('Maula Case Officer', 'maula.case', 'Case Officer'),
    ('Maula Disciplinary Officer', 'maula.discipline', 'Disciplinary Officer'),
    ('Maula Program Facilitator', 'maula.program', 'Program Facilitator'),
    ('Maula Prison Officer', 'maula.officer', 'Prison Officer'),
    ('Chichiri Records Officer', 'chichiri.records', 'Records Officer'),
    ('Chichiri Medical Officer', 'chichiri.medical', 'Medical Officer'),
    ('Chichiri Duty Officer', 'chichiri.duty', 'Duty Officer'),
    ('Chichiri Visitor Officer', 'chichiri.visitor', 'Visitor Officer'),
    ('Chichiri Case Officer', 'chichiri.case', 'Case Officer'),
    ('Chichiri Disciplinary Officer', 'chichiri.discipline', 'Disciplinary Officer'),
    ('Chichiri Program Facilitator', 'chichiri.program', 'Program Facilitator'),
    ('Chichiri Prison Officer', 'chichiri.officer', 'Prison Officer')
) AS u(full_name, username, role)
WHERE p.code = CASE WHEN u.username LIKE 'maula.%' THEN 'MAULA' ELSE 'CHICHIRI' END
ON CONFLICT (username) DO NOTHING;

-- Two current inmates per prison, each assigned to a demo cell.
INSERT INTO inmates (inmate_number, first_name, last_name, date_of_birth, gender, admission_date,
                     classification, current_facility, current_prison_id, offence,
                     sentence_duration_value, sentence_duration_unit, sentence_start_date, sentence_end_date)
SELECT v.inmate_number, v.first_name, v.last_name, v.dob, v.gender, DATE '2025-01-15',
       'Medium', p.name, p.id, v.offence, 5, 'YEARS', DATE '2025-01-15', DATE '2030-01-15'
FROM prisons p
CROSS JOIN (VALUES
    ('MAU-DEMO-001', 'Peter', 'Banda', DATE '1995-04-12', 'Male', 'Theft'),
    ('MAU-DEMO-002', 'Grace', 'Phiri', DATE '1998-09-20', 'Female', 'Fraud'),
    ('CHI-DEMO-001', 'James', 'Zulu', DATE '1992-02-03', 'Male', 'Assault'),
    ('CHI-DEMO-002', 'Mary', 'Chirwa', DATE '1996-07-08', 'Female', 'Burglary')
) AS v(inmate_number, first_name, last_name, dob, gender, offence)
WHERE (p.code = 'MAULA' AND v.inmate_number LIKE 'MAU-%')
   OR (p.code = 'CHICHIRI' AND v.inmate_number LIKE 'CHI-%')
ON CONFLICT (inmate_number) DO NOTHING;

INSERT INTO cell_allocations (inmate_id, cell_id)
SELECT i.id, c.id FROM inmates i
JOIN prisons p ON p.id = i.current_prison_id
JOIN blocks b ON b.prison_id = p.id AND b.name = 'A'
JOIN cells c ON c.block_id = b.id AND c.cell_number = CASE WHEN i.inmate_number LIKE '%001' THEN 1 ELSE 2 END
WHERE i.inmate_number IN ('MAU-DEMO-001','MAU-DEMO-002','CHI-DEMO-001','CHI-DEMO-002')
ON CONFLICT DO NOTHING;

INSERT INTO "Case" (inmate_id, case_number, court, case_start_date, status, recorded_by)
SELECT i.id, 'DEMO-' || i.inmate_number, 'Lilongwe/Blantyre Magistrate Court', DATE '2024-12-01', 'ongoing', 'seed-script'
FROM inmates i WHERE i.inmate_number IN ('MAU-DEMO-001','MAU-DEMO-002','CHI-DEMO-001','CHI-DEMO-002')
ON CONFLICT DO NOTHING;

INSERT INTO Crime (offence_name, description, category)
SELECT DISTINCT i.offence, 'Demo offence record', 'General'
FROM inmates i WHERE i.inmate_number IN ('MAU-DEMO-001','MAU-DEMO-002','CHI-DEMO-001','CHI-DEMO-002')
AND NOT EXISTS (SELECT 1 FROM Crime cr WHERE cr.offence_name = i.offence);

INSERT INTO "CaseCrime" (case_id, crime_id)
SELECT ca.case_id, cr.crime_id FROM "Case" ca
JOIN inmates i ON i.id = ca.inmate_id JOIN Crime cr ON cr.offence_name = i.offence
WHERE ca.case_number LIKE 'DEMO-%' ON CONFLICT DO NOTHING;

INSERT INTO Sentence (case_id, inmate_id, duration, start_date, release_date, status)
SELECT ca.case_id, i.id, 5, DATE '2025-01-15', DATE '2030-01-15', 'active'
FROM "Case" ca JOIN inmates i ON i.id = ca.inmate_id
WHERE ca.case_number LIKE 'DEMO-%'
AND NOT EXISTS (SELECT 1 FROM Sentence s WHERE s.case_id = ca.case_id);

INSERT INTO HealthRecords (inmate_id, screening_date, screening_results, blood_group, created_by, status)
SELECT i.id, CURRENT_DATE, 'Routine demo screening: stable', 'O+', 'seed-script', 'ongoing'
FROM inmates i WHERE i.inmate_number IN ('MAU-DEMO-001','MAU-DEMO-002','CHI-DEMO-001','CHI-DEMO-002')
AND NOT EXISTS (SELECT 1 FROM HealthRecords h WHERE h.inmate_id = i.id);

INSERT INTO Incident (incident_date, description, status, reported_by)
SELECT DATE '2026-01-10', 'Demo incident for ' || i.inmate_number, 'open', 'seed-script'
FROM inmates i WHERE i.inmate_number IN ('MAU-DEMO-001','CHI-DEMO-001')
AND NOT EXISTS (SELECT 1 FROM Incident x WHERE x.description = 'Demo incident for ' || i.inmate_number);

INSERT INTO IncidentInmate (incident_id, inmate_id, role_in_incident)
SELECT x.incident_id, i.id, 'involved'
FROM Incident x JOIN inmates i ON x.description = 'Demo incident for ' || i.inmate_number
WHERE x.description LIKE 'Demo incident for %' ON CONFLICT DO NOTHING;

INSERT INTO Visitor (full_name, id_number, phone, relationship_to_inmate)
SELECT 'Demo Visitor ' || i.inmate_number, 'VIS-' || i.inmate_number, '+265000000000', 'Relative'
FROM inmates i WHERE i.inmate_number IN ('MAU-DEMO-001','CHI-DEMO-001')
AND NOT EXISTS (SELECT 1 FROM Visitor v WHERE v.id_number = 'VIS-' || i.inmate_number);

INSERT INTO Visit (visitor_id, visit_datetime, duration, logged_by)
SELECT v.visitor_id, CURRENT_DATE, 60, 'seed-script' FROM Visitor v
WHERE v.id_number IN ('VIS-MAU-DEMO-001','VIS-CHI-DEMO-001')
AND NOT EXISTS (SELECT 1 FROM Visit x WHERE x.visitor_id = v.visitor_id AND x.visit_datetime = CURRENT_DATE);

INSERT INTO RehabProgram (name, description, instructor, schedule, status)
VALUES ('Demo Literacy', 'Introductory literacy program', 'Demo Facilitator', 'Mon/Wed 09:00', 'active'),
       ('Demo Carpentry', 'Basic carpentry skills', 'Demo Facilitator', 'Tue/Thu 10:00', 'active')
ON CONFLICT DO NOTHING;

INSERT INTO Enrollment (inmate_id, program_id, enrollment_date, status)
SELECT i.id, rp.program_id, CURRENT_DATE, 'active' FROM inmates i
CROSS JOIN LATERAL (SELECT program_id FROM RehabProgram WHERE name = 'Demo Literacy' ORDER BY program_id LIMIT 1) rp
WHERE i.inmate_number IN ('MAU-DEMO-001','CHI-DEMO-001')
AND NOT EXISTS (SELECT 1 FROM Enrollment e WHERE e.inmate_id = i.id AND e.program_id = rp.program_id);

INSERT INTO AttendanceRecord (enrollment_id, session_date, present, remarks, recorded_by)
SELECT e.enrollment_id, CURRENT_DATE, 'Y', 'Demo attendance', 'seed-script'
FROM Enrollment e JOIN inmates i ON i.id = e.inmate_id
WHERE i.inmate_number IN ('MAU-DEMO-001','CHI-DEMO-001')
AND NOT EXISTS (SELECT 1 FROM AttendanceRecord ar WHERE ar.enrollment_id=e.enrollment_id AND ar.session_date=CURRENT_DATE);
