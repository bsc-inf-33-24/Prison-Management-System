CREATE TABLE inmate_offences (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    inmate_id BIGINT NOT NULL REFERENCES inmates(id) ON DELETE RESTRICT,
    offence VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inmate_offences_inmate_created
    ON inmate_offences(inmate_id, created_at DESC, id DESC);
