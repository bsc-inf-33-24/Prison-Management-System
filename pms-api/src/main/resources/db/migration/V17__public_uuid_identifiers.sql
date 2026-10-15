CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE users ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE users SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE users ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_public_uuid ON users (public_uuid);

ALTER TABLE inmates ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE inmates SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE inmates ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_inmates_public_uuid ON inmates (public_uuid);

ALTER TABLE cells ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE cells SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE cells ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cells_public_uuid ON cells (public_uuid);

ALTER TABLE cell_allocations ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE cell_allocations SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE cell_allocations ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cell_allocations_public_uuid ON cell_allocations (public_uuid);

ALTER TABLE incident ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE incident SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE incident ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_incident_public_uuid ON incident (public_uuid);

ALTER TABLE incident_inmate ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE incident_inmate SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE incident_inmate ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_incident_inmate_public_uuid ON incident_inmate (public_uuid);

ALTER TABLE visitors ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE visitors SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE visitors ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_visitors_public_uuid ON visitors (public_uuid);

ALTER TABLE visits ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE visits SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE visits ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_visits_public_uuid ON visits (public_uuid);

ALTER TABLE cases ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE cases SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE cases ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cases_public_uuid ON cases (public_uuid);

ALTER TABLE crimes ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE crimes SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE crimes ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_crimes_public_uuid ON crimes (public_uuid);

ALTER TABLE sentences ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE sentences SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE sentences ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sentences_public_uuid ON sentences (public_uuid);

ALTER TABLE rehabprogram ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE rehabprogram SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE rehabprogram ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_rehabprogram_public_uuid ON rehabprogram (public_uuid);

ALTER TABLE enrollment ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE enrollment SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE enrollment ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_enrollment_public_uuid ON enrollment (public_uuid);

ALTER TABLE attendancerecord ADD COLUMN IF NOT EXISTS public_uuid UUID;
UPDATE attendancerecord SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
ALTER TABLE attendancerecord ALTER COLUMN public_uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_attendancerecord_public_uuid ON attendancerecord (public_uuid);
