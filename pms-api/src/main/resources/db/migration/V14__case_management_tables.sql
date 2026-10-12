CREATE TABLE IF NOT EXISTS crimes (
    id BIGSERIAL PRIMARY KEY,
    offence_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cases (
    id BIGSERIAL PRIMARY KEY,
    inmate_id BIGINT NOT NULL REFERENCES inmates(id) ON DELETE RESTRICT,
    case_number VARCHAR(80) NOT NULL UNIQUE,
    court VARCHAR(150) NOT NULL,
    case_start_date DATE NOT NULL,
    case_end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ongoing',
    created_by BIGINT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS case_crimes (
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    crime_id BIGINT NOT NULL REFERENCES crimes(id) ON DELETE RESTRICT,
    PRIMARY KEY (case_id, crime_id),
    CONSTRAINT uk_case_crimes_case_crime UNIQUE (case_id, crime_id)
);

CREATE TABLE IF NOT EXISTS sentences (
    sentence_id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE RESTRICT,
    inmate_id BIGINT NOT NULL REFERENCES inmates(id) ON DELETE RESTRICT,
    duration INTEGER NOT NULL,
    start_date DATE NOT NULL,
    release_date DATE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION set_case_management_timestamps() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.created_at := CURRENT_TIMESTAMP;
    ELSE
        NEW.created_at := OLD.created_at;
    END IF;
    NEW.updated_at := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'crimes_set_timestamps'
          AND tgrelid = 'crimes'::regclass
          AND NOT tgisinternal
    ) THEN
        CREATE TRIGGER crimes_set_timestamps
            BEFORE INSERT OR UPDATE ON crimes
            FOR EACH ROW EXECUTE FUNCTION set_case_management_timestamps();
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'cases_set_timestamps'
          AND tgrelid = 'cases'::regclass
          AND NOT tgisinternal
    ) THEN
        CREATE TRIGGER cases_set_timestamps
            BEFORE INSERT OR UPDATE ON cases
            FOR EACH ROW EXECUTE FUNCTION set_case_management_timestamps();
    END IF;
END;
$$;
