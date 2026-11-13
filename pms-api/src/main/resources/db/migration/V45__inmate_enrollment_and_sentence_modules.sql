ALTER TABLE inmates
    ADD COLUMN inmate_type VARCHAR(20) NOT NULL DEFAULT 'SENTENCED',
    ADD COLUMN bio TEXT NOT NULL DEFAULT 'Legacy record',
    ADD COLUMN medical_history TEXT NOT NULL DEFAULT 'Not recorded for legacy record',
    ADD COLUMN expected_court_date DATE,
    ADD COLUMN next_of_kin_name VARCHAR(200),
    ADD COLUMN next_of_kin_relationship VARCHAR(100),
    ADD COLUMN next_of_kin_phone VARCHAR(40),
    ADD COLUMN next_of_kin_email VARCHAR(254),
    ADD COLUMN contact_phone VARCHAR(40),
    ADD COLUMN contact_email VARCHAR(254),
    ADD CONSTRAINT ck_inmates_type CHECK (inmate_type IN ('REMAND', 'SENTENCED'));

UPDATE inmates SET inmate_type = 'REMAND'
WHERE sentence_start_date IS NULL AND sentence_end_date IS NULL;

CREATE TABLE inmate_sentences (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    inmate_id BIGINT NOT NULL REFERENCES inmates(id) ON DELETE RESTRICT,
    offence VARCHAR(500) NOT NULL,
    sentence_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_inmate_sentences_type CHECK (sentence_type IN ('FIXED_TERM', 'LIFE', 'UNDETERMINED')),
    CONSTRAINT ck_inmate_sentences_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'COMMUTED')),
    CONSTRAINT ck_inmate_sentences_dates CHECK (
        (sentence_type = 'FIXED_TERM' AND end_date IS NOT NULL AND end_date >= start_date)
        OR (sentence_type IN ('LIFE', 'UNDETERMINED') AND end_date IS NULL))
);
CREATE INDEX idx_inmate_sentences_inmate ON inmate_sentences(inmate_id);

CREATE TABLE sentence_remissions (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    sentence_id BIGINT NOT NULL REFERENCES inmate_sentences(id) ON DELETE RESTRICT,
    remission_percentage NUMERIC(5,2) NOT NULL,
    effective_date DATE NOT NULL,
    reason TEXT NOT NULL,
    approved_by_user_id BIGINT REFERENCES users(user_id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sentence_remissions_percentage CHECK (remission_percentage BETWEEN 0 AND 100)
);
CREATE INDEX idx_sentence_remissions_sentence ON sentence_remissions(sentence_id);

CREATE TABLE court_date_reminder_deliveries (
    id BIGSERIAL PRIMARY KEY,
    inmate_id BIGINT NOT NULL REFERENCES inmates(id) ON DELETE RESTRICT,
    court_date DATE NOT NULL,
    days_before INTEGER NOT NULL,
    channel VARCHAR(10) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_court_reminder_delivery UNIQUE (inmate_id, court_date, days_before, channel),
    CONSTRAINT ck_court_reminder_channel CHECK (channel IN ('IN_APP', 'EMAIL'))
);

INSERT INTO inmate_sentences (public_uuid, inmate_id, offence, sentence_type, start_date, end_date, status)
SELECT gen_random_uuid(), id, COALESCE(offence, 'Legacy sentence'), sentence_type,
       sentence_start_date, sentence_end_date, 'ACTIVE'
FROM inmates
WHERE sentence_start_date IS NOT NULL;

INSERT INTO sentence_remissions (public_uuid, sentence_id, remission_percentage, effective_date, reason)
SELECT gen_random_uuid(), s.id, i.remission_percentage, i.sentence_start_date, 'Migrated from legacy inmate remission'
FROM inmates i JOIN inmate_sentences s ON s.inmate_id = i.id
WHERE i.remission_percentage IS NOT NULL;

DROP INDEX IF EXISTS idx_inmates_projected_release_date;

ALTER TABLE inmates
    DROP CONSTRAINT IF EXISTS ck_inmates_sentence_details,
    DROP CONSTRAINT IF EXISTS ck_inmates_sentence_type,
    DROP CONSTRAINT IF EXISTS ck_inmates_remission_percentage,
    DROP CONSTRAINT IF EXISTS ck_inmates_sentence_dates_ordered,
    DROP COLUMN offence,
    DROP COLUMN sentence_duration_value,
    DROP COLUMN sentence_duration_unit,
    DROP COLUMN sentence_start_date,
    DROP COLUMN sentence_end_date,
    DROP COLUMN sentence_type,
    DROP COLUMN remission_percentage,
    DROP COLUMN projected_release_date,
    DROP COLUMN release_reminder_sent_at;
