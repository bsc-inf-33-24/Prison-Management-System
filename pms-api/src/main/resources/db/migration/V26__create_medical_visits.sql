CREATE TABLE medical_visits (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    inmate_id BIGINT NOT NULL REFERENCES inmates (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    recorded_by_user_id BIGINT NOT NULL REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    visit_date DATE NOT NULL,
    sickness TEXT NOT NULL,
    diagnosis TEXT NOT NULL,
    medications_provided TEXT,
    recommendations TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_medical_visits_inmate_date
    ON medical_visits (inmate_id, visit_date DESC, id DESC);
