-- Add public UUIDs and standard timestamps to existing health tables.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE healthrecords
    ADD COLUMN IF NOT EXISTS public_uuid UUID,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE medicalcondition
    ADD COLUMN IF NOT EXISTS public_uuid UUID,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE consultation
    ADD COLUMN IF NOT EXISTS public_uuid UUID,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE medication
    ADD COLUMN IF NOT EXISTS public_uuid UUID,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE medicalappointment
    ADD COLUMN IF NOT EXISTS public_uuid UUID,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- Backfill UUIDs for existing health records.
UPDATE healthrecords SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
UPDATE medicalcondition SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
UPDATE consultation SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
UPDATE medication SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;
UPDATE medicalappointment SET public_uuid = gen_random_uuid() WHERE public_uuid IS NULL;

-- Make UUIDs mandatory and unique.
ALTER TABLE healthrecords ALTER COLUMN public_uuid SET NOT NULL;
ALTER TABLE medicalcondition ALTER COLUMN public_uuid SET NOT NULL;
ALTER TABLE consultation ALTER COLUMN public_uuid SET NOT NULL;
ALTER TABLE medication ALTER COLUMN public_uuid SET NOT NULL;
ALTER TABLE medicalappointment ALTER COLUMN public_uuid SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_healthrecords_public_uuid ON healthrecords(public_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_medicalcondition_public_uuid ON medicalcondition(public_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_consultation_public_uuid ON consultation(public_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_medication_public_uuid ON medication(public_uuid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_medicalappointment_public_uuid ON medicalappointment(public_uuid);

-- Link new health records to the current inmates table.
-- The legacy inmate_id column remains untouched for compatibility.
ALTER TABLE healthrecords
    ADD COLUMN IF NOT EXISTS current_inmate_id BIGINT;

ALTER TABLE healthrecords
    ADD CONSTRAINT fk_healthrecords_current_inmate
    FOREIGN KEY (current_inmate_id) REFERENCES inmates(id)
    ON DELETE SET NULL;

-- Create the missing allergies resource.
CREATE TABLE allergies (
    allergy_id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    current_inmate_id BIGINT NOT NULL REFERENCES inmates(id) ON DELETE CASCADE,
    allergen VARCHAR(255) NOT NULL,
    reaction TEXT,
    severity VARCHAR(50),
    recorded_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'active'
        CHECK (status IN ('active', 'resolved')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_healthrecords_current_inmate_id
    ON healthrecords(current_inmate_id);

CREATE INDEX idx_allergies_current_inmate_id
    ON allergies(current_inmate_id);
