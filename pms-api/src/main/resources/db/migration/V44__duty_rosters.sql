CREATE TABLE roster (
    roster_id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    prison_id BIGINT NOT NULL REFERENCES prisons(id) ON DELETE RESTRICT,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_roster_period CHECK (period_end >= period_start),
    CONSTRAINT chk_roster_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED'))
);

CREATE TABLE roster_shift (
    roster_shift_id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    roster_id BIGINT NOT NULL REFERENCES roster(roster_id) ON DELETE CASCADE,
    assigned_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    location VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_roster_shift_duration CHECK (
        end_at > start_at AND end_at <= start_at + INTERVAL '24 hours'
    ),
    CONSTRAINT chk_roster_shift_location CHECK (length(trim(location)) > 0)
);

CREATE INDEX idx_roster_prison_period ON roster (prison_id, period_start, period_end);
CREATE INDEX idx_roster_shift_officer_time ON roster_shift (assigned_user_id, start_at, end_at);
CREATE INDEX idx_roster_shift_roster_time ON roster_shift (roster_id, start_at);
