CREATE TABLE IF NOT EXISTS visitors (
    visitor_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    id_number VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    relationship_to_inmate VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_visitors_id_number ON visitors (id_number);

CREATE TABLE IF NOT EXISTS visits (
    visit_id BIGSERIAL PRIMARY KEY,
    visitor_id BIGINT NOT NULL REFERENCES visitors (visitor_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    inmate_id BIGINT NOT NULL REFERENCES inmates (id) ON UPDATE CASCADE ON DELETE RESTRICT,
    visit_datetime TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes >= 0 AND duration_minutes <= 1440),
    logged_by BIGINT NOT NULL REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visits_inmate_id ON visits (inmate_id);
CREATE INDEX IF NOT EXISTS idx_visits_visitor_id ON visits (visitor_id);
CREATE INDEX IF NOT EXISTS idx_visits_datetime ON visits (visit_datetime);
