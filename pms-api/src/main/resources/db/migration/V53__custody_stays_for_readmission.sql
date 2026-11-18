CREATE TABLE custody_stays (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    inmate_id BIGINT NOT NULL REFERENCES inmates(id) ON DELETE RESTRICT,
    prison_id BIGINT NOT NULL REFERENCES prisons(id) ON DELETE RESTRICT,
    admission_date DATE NOT NULL,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_custody_stays_inmate ON custody_stays(inmate_id, id DESC);
CREATE UNIQUE INDEX uk_custody_stays_active_inmate ON custody_stays(inmate_id) WHERE ended_at IS NULL;

INSERT INTO custody_stays (public_uuid, inmate_id, prison_id, admission_date, ended_at, created_at, updated_at)
SELECT gen_random_uuid(), i.id, i.current_prison_id, i.admission_date,
       CASE WHEN EXISTS (SELECT 1 FROM cell_allocations a WHERE a.inmate_id = i.id AND a.released_at IS NULL)
            THEN NULL ELSE NOW() END,
       NOW(), NOW()
FROM inmates i WHERE i.current_prison_id IS NOT NULL;
