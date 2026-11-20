-- Repeatable local demo data for the current Flyway schema (V53).
-- Run with: psql -v ON_ERROR_STOP=1 -f scripts/seed-current-schema.sql
-- Demo account password: DemoPMS2026!
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
SELECT gen_random_uuid(), b.name, x.n, 4, b.id FROM blocks b
JOIN prisons p ON p.id=b.prison_id CROSS JOIN (VALUES (1),(2)) x(n)
WHERE p.code IN ('MAULA','CHICHIRI')
ON CONFLICT (block_id,cell_number) WHERE block_id IS NOT NULL DO NOTHING;

INSERT INTO users (public_uuid,full_name,username,password_hash,role,prison_id)
SELECT gen_random_uuid(),u.full_name,u.username,crypt('DemoPMS2026!',gen_salt('bf')),u.role,p.id
FROM prisons p CROSS JOIN (VALUES
 ('Maula Records Officer','maula.records','Records Officer'),
 ('Maula Medical Officer','maula.medical','Medical Officer'),
 ('Maula Visitor Officer','maula.visitor','Visitor Officer'),
 ('Maula Case Officer','maula.case','Case Officer'),
 ('Maula Disciplinary Officer','maula.discipline','Disciplinary Officer'),
 ('Maula Program Facilitator','maula.program','Program Facilitator'),
 ('Chichiri Records Officer','chichiri.records','Records Officer'),
 ('Chichiri Medical Officer','chichiri.medical','Medical Officer'),
 ('Chichiri Visitor Officer','chichiri.visitor','Visitor Officer'),
 ('Chichiri Case Officer','chichiri.case','Case Officer'),
 ('Chichiri Disciplinary Officer','chichiri.discipline','Disciplinary Officer'),
 ('Chichiri Program Facilitator','chichiri.program','Program Facilitator')
) u(full_name,username,role)
WHERE p.code=CASE WHEN u.username LIKE 'maula.%' THEN 'MAULA' ELSE 'CHICHIRI' END
ON CONFLICT (username) DO NOTHING;

INSERT INTO inmates (public_uuid,first_name,last_name,date_of_birth,gender,admission_date,current_prison_id,
 inmate_type,bio,medical_history,home_of_origin)
SELECT gen_random_uuid(),x.first_name,x.last_name,x.dob,x.gender,DATE '2025-01-15',p.id,
 'SENTENCED','Demo inmate record','No known medical concerns',x.origin
FROM prisons p CROSS JOIN (VALUES
 ('Peter','Banda',DATE '1995-04-12','Male','Theft','Lilongwe'),
 ('Grace','Phiri',DATE '1998-09-20','Female','Fraud','Lilongwe'),
 ('James','Zulu',DATE '1992-02-03','Male','Assault','Blantyre'),
 ('Mary','Chirwa',DATE '1996-07-08','Female','Burglary','Blantyre')
) x(first_name,last_name,dob,gender,offence,origin)
WHERE p.code=CASE WHEN x.origin='Lilongwe' THEN 'MAULA' ELSE 'CHICHIRI' END
AND NOT EXISTS (SELECT 1 FROM inmates i WHERE i.first_name=x.first_name AND i.last_name=x.last_name AND i.current_prison_id=p.id);

INSERT INTO custody_stays(public_uuid,inmate_id,prison_id,admission_date)
SELECT gen_random_uuid(),i.id,i.current_prison_id,i.admission_date FROM inmates i
WHERE i.last_name IN ('Banda','Phiri','Zulu','Chirwa') AND NOT EXISTS
(SELECT 1 FROM custody_stays cs WHERE cs.inmate_id=i.id AND cs.ended_at IS NULL);

INSERT INTO cell_allocations(public_uuid,inmate_id,cell_id)
SELECT gen_random_uuid(),i.id,c.id FROM inmates i JOIN blocks b ON b.prison_id=i.current_prison_id AND b.name='A'
JOIN cells c ON c.block_id=b.id AND c.cell_number=1 WHERE i.last_name IN ('Banda','Phiri','Zulu','Chirwa')
AND NOT EXISTS (SELECT 1 FROM cell_allocations a WHERE a.inmate_id=i.id AND a.released_at IS NULL);

INSERT INTO inmate_sentences(public_uuid,inmate_id,offence,sentence_type,start_date,end_date)
SELECT gen_random_uuid(),i.id,x.offence,'FIXED_TERM',DATE '2025-01-15',DATE '2030-01-15'
FROM inmates i JOIN (VALUES ('Banda','Theft'),('Phiri','Fraud'),('Zulu','Assault'),('Chirwa','Burglary')) x(last_name,offence) USING(last_name)
WHERE NOT EXISTS (SELECT 1 FROM inmate_sentences s WHERE s.inmate_id=i.id);

INSERT INTO cases(public_uuid,inmate_id,case_number,court,case_start_date,case_end_date,status,created_by,crimes)
SELECT gen_random_uuid(),i.id,'DEMO-'||p.code||'-'||i.last_name,'Magistrate Court',DATE '2025-01-10',DATE '2030-01-15','ongoing',u.user_id,s.offence
FROM inmates i JOIN prisons p ON p.id=i.current_prison_id JOIN users u ON u.prison_id=p.id AND u.role='Case Officer'
JOIN inmate_sentences s ON s.inmate_id=i.id WHERE i.last_name IN ('Banda','Phiri','Zulu','Chirwa')
AND NOT EXISTS (SELECT 1 FROM cases c WHERE c.case_number='DEMO-'||p.code||'-'||i.last_name);

COMMIT;
