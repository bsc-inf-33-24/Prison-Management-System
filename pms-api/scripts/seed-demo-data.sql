-- Repeatable local demo seed. Requires Flyway migrations through V43.
-- Invoke with psql -v ON_ERROR_STOP=1 so any schema mismatch stops the script.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
BEGIN;

INSERT INTO prisons (public_uuid, code, name, location)
VALUES ('10000000-0000-4000-8000-000000000001','MAULA','Maula Prison','Lilongwe'),
       ('10000000-0000-4000-8000-000000000002','CHICHIRI','Chichiri Prison','Blantyre')
ON CONFLICT (code) DO NOTHING;

INSERT INTO blocks (public_uuid, prison_id, name)
SELECT gen_random_uuid(), p.id, x.name FROM prisons p
CROSS JOIN (VALUES ('A'),('B')) x(name) WHERE p.code IN ('MAULA','CHICHIRI')
ON CONFLICT (prison_id,name) DO NOTHING;

INSERT INTO cells (public_uuid, block_name, cell_number, capacity, block_id)
SELECT gen_random_uuid(), b.name, x.n, 4, b.id
FROM blocks b JOIN prisons p ON p.id=b.prison_id CROSS JOIN (VALUES (1),(2)) x(n)
WHERE p.code IN ('MAULA','CHICHIRI')
ON CONFLICT (block_id,cell_number) WHERE block_id IS NOT NULL DO NOTHING;

-- Local demo accounts, password is documented in DEMO-CREDENTIALS.txt.
INSERT INTO users (public_uuid,full_name,username,password_hash,role,prison_id)
SELECT gen_random_uuid(), u.full_name,u.username,crypt('DemoPMS2026!',gen_salt('bf')),u.role,p.id
FROM prisons p CROSS JOIN (VALUES
 ('Maula Records Officer','maula.records','Records Officer'),
 ('Maula Medical Officer','maula.medical','Medical Officer'),
 ('Maula Duty Officer','maula.duty','Duty Officer'),
 ('Maula Visitor Officer','maula.visitor','Visitor Officer'),
 ('Maula Case Officer','maula.case','Case Officer'),
 ('Maula Disciplinary Officer','maula.discipline','Disciplinary Officer'),
 ('Maula Program Facilitator','maula.program','Program Facilitator'),
 ('Maula Prison Officer','maula.officer','Prison Officer'),
 ('Chichiri Records Officer','chichiri.records','Records Officer'),
 ('Chichiri Medical Officer','chichiri.medical','Medical Officer'),
 ('Chichiri Duty Officer','chichiri.duty','Duty Officer'),
 ('Chichiri Visitor Officer','chichiri.visitor','Visitor Officer'),
 ('Chichiri Case Officer','chichiri.case','Case Officer'),
 ('Chichiri Disciplinary Officer','chichiri.discipline','Disciplinary Officer'),
 ('Chichiri Program Facilitator','chichiri.program','Program Facilitator'),
 ('Chichiri Prison Officer','chichiri.officer','Prison Officer')
) u(full_name,username,role)
WHERE p.code=CASE WHEN u.username LIKE 'maula.%' THEN 'MAULA' ELSE 'CHICHIRI' END
ON CONFLICT (username) DO NOTHING;

-- Current schema intentionally has no inmate_number; use names plus prison assignment for stable matching.
INSERT INTO inmates (public_uuid,first_name,last_name,date_of_birth,gender,admission_date,
 current_prison_id,offence,sentence_duration_value,sentence_duration_unit,
 sentence_start_date,sentence_end_date,projected_release_date)
SELECT gen_random_uuid(),x.first_name,x.last_name,x.dob,x.gender,DATE '2025-01-15',p.id,
 x.offence,5,'YEARS',DATE '2025-01-15',DATE '2030-01-15',DATE '2030-01-15'
FROM prisons p CROSS JOIN (VALUES
 ('Peter','Banda',DATE '1995-04-12','Male','Theft','MAULA'),
 ('Grace','Phiri',DATE '1998-09-20','Female','Fraud','MAULA'),
 ('James','Zulu',DATE '1992-02-03','Male','Assault','CHICHIRI'),
 ('Mary','Chirwa',DATE '1996-07-08','Female','Burglary','CHICHIRI')
) x(first_name,last_name,dob,gender,offence,prison_code)
WHERE p.code=x.prison_code AND NOT EXISTS (
 SELECT 1 FROM inmates i WHERE i.first_name=x.first_name AND i.last_name=x.last_name AND i.current_prison_id=p.id);

INSERT INTO cell_allocations (public_uuid,inmate_id,cell_id)
SELECT gen_random_uuid(),i.id,c.id FROM inmates i JOIN prisons p ON p.id=i.current_prison_id
JOIN blocks b ON b.prison_id=p.id AND b.name='A'
JOIN cells c ON c.block_id=b.id AND c.cell_number=1
WHERE i.last_name IN ('Banda','Phiri','Zulu','Chirwa')
AND NOT EXISTS (SELECT 1 FROM cell_allocations a WHERE a.inmate_id=i.id AND a.released_at IS NULL);

-- Current migrations retain the legacy singular crime table; cases store offence text.
INSERT INTO crime (offence_name,description,category)
SELECT v.offence,'Demo offence record','General' FROM (VALUES ('Theft'),('Fraud'),('Assault'),('Burglary')) v(offence)
WHERE NOT EXISTS (SELECT 1 FROM crime c WHERE c.offence_name=v.offence);
INSERT INTO cases (public_uuid,inmate_id,case_number,court,case_start_date,case_end_date,status,created_by,crimes)
SELECT gen_random_uuid(),i.id,'DEMO-'||p.code||'-'||i.last_name,
 'Magistrate Court',DATE '2025-01-10',DATE '2030-01-15','ongoing',u.user_id,i.offence
FROM inmates i JOIN prisons p ON p.id=i.current_prison_id
JOIN users u ON u.prison_id=p.id AND u.role='Case Officer'
WHERE i.last_name IN ('Banda','Phiri','Zulu','Chirwa')
AND NOT EXISTS (SELECT 1 FROM cases c WHERE c.case_number='DEMO-'||p.code||'-'||i.last_name);
INSERT INTO sentences (public_uuid,case_id,inmate_id,duration,start_date,release_date,status)
SELECT gen_random_uuid(),c.id,c.inmate_id,5,DATE '2025-01-15',DATE '2030-01-15','active'
FROM cases c WHERE c.case_number LIKE 'DEMO-%'
AND NOT EXISTS (SELECT 1 FROM sentences s WHERE s.case_id=c.id);

-- Medical module uses the current medical_visits table (legacy HealthRecords
-- still points at the retired singular inmate table and has no public UUID).
INSERT INTO medical_visits (public_uuid,inmate_id,recorded_by_user_id,visit_date,sickness,diagnosis,medications_provided,recommendations)
SELECT gen_random_uuid(),i.id,u.user_id,CURRENT_DATE,'Routine check','No acute concerns','None','Routine follow-up'
FROM inmates i JOIN users u ON u.prison_id=i.current_prison_id AND u.role='Medical Officer'
WHERE i.last_name IN ('Banda','Phiri','Zulu','Chirwa')
AND NOT EXISTS (SELECT 1 FROM medical_visits m WHERE m.inmate_id=i.id);

-- Discipline/incidents module.
INSERT INTO incident (public_uuid,incident_date,description,status,reported_by)
SELECT gen_random_uuid(),CURRENT_DATE,'Demo incident: '||i.last_name,'open','seed-script'
FROM inmates i WHERE i.last_name IN ('Banda','Zulu')
AND NOT EXISTS (SELECT 1 FROM incident x WHERE x.description='Demo incident: '||i.last_name);
INSERT INTO incident_inmate (public_uuid,incident_id,inmate_id,role_in_incident)
SELECT gen_random_uuid(),x.incident_id,i.id,'involved' FROM incident x JOIN inmates i ON x.description='Demo incident: '||i.last_name
WHERE x.description LIKE 'Demo incident:%' ON CONFLICT DO NOTHING;

-- Visitor/visit module (current denormalized visit schema).
INSERT INTO visits (public_uuid,inmate_id,visit_datetime,visit_end_datetime,duration_minutes,logged_by,visitor_full_name)
SELECT gen_random_uuid(),i.id,NOW(),NOW()+INTERVAL '60 minutes',60,u.user_id,'Demo Visitor '||i.last_name
FROM inmates i JOIN prisons p ON p.id=i.current_prison_id
JOIN users u ON u.prison_id=p.id AND u.role='Visitor Officer'
WHERE i.last_name IN ('Banda','Zulu')
AND NOT EXISTS (SELECT 1 FROM visits v WHERE v.inmate_id=i.id AND v.visitor_full_name='Demo Visitor '||i.last_name);

-- Rehabilitation module.
INSERT INTO rehabprogram (public_uuid,name,description,instructor,schedule,status)
VALUES (gen_random_uuid(),'Demo Literacy','Introductory literacy program','Demo Facilitator','Mon/Wed 09:00','active'),
       (gen_random_uuid(),'Demo Carpentry','Basic carpentry skills','Demo Facilitator','Tue/Thu 10:00','active')
ON CONFLICT DO NOTHING;
INSERT INTO enrollment (public_uuid,inmate_id,program_id,enrollment_date,status)
SELECT gen_random_uuid(),i.id,r.program_id,CURRENT_DATE,'active' FROM inmates i
CROSS JOIN LATERAL (SELECT program_id FROM rehabprogram WHERE name='Demo Literacy' ORDER BY program_id LIMIT 1) r
WHERE i.last_name IN ('Banda','Zulu')
AND NOT EXISTS (SELECT 1 FROM enrollment e WHERE e.inmate_id=i.id AND e.program_id=r.program_id);
INSERT INTO attendancerecord (public_uuid,enrollment_id,session_date,present,remarks,recorded_by)
SELECT gen_random_uuid(),e.enrollment_id,CURRENT_DATE,'Y','Demo attendance','seed-script'
FROM enrollment e JOIN inmates i ON i.id=e.inmate_id WHERE i.last_name IN ('Banda','Zulu')
AND NOT EXISTS (SELECT 1 FROM attendancerecord a WHERE a.enrollment_id=e.enrollment_id AND a.session_date=CURRENT_DATE);

COMMIT;
