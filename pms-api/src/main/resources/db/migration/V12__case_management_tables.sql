DROP TABLE IF EXISTS sentence;
DROP TABLE IF EXISTS casecrime;
DROP TABLE IF EXISTS "Case";
DROP TABLE IF EXISTS crime;

CREATE TABLE crimes (
    id BIGSERIAL PRIMARY KEY,
    offence_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cases (
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

CREATE TABLE case_crimes (
    case_id BIGINT NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    crime_id BIGINT NOT NULL REFERENCES crimes(id) ON DELETE RESTRICT,
    PRIMARY KEY (case_id, crime_id),
    UNIQUE (case_id, crime_id)
);

CREATE FUNCTION set_case_management_timestamps() RETURNS trigger AS $$
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

CREATE TRIGGER crimes_set_timestamps
    BEFORE INSERT OR UPDATE ON crimes
    FOR EACH ROW EXECUTE FUNCTION set_case_management_timestamps();

CREATE TRIGGER cases_set_timestamps
    BEFORE INSERT OR UPDATE ON cases
    FOR EACH ROW EXECUTE FUNCTION set_case_management_timestamps();
