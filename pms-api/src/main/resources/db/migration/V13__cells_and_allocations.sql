DO $$
BEGIN
    IF to_regclass('public.incidentinmate') IS NOT NULL
       AND to_regclass('public.incident_inmate') IS NULL THEN
        ALTER TABLE incidentinmate RENAME TO incident_inmate;
    ELSIF to_regclass('public.incidentinmate') IS NOT NULL
       AND to_regclass('public.incident_inmate') IS NOT NULL THEN
        RAISE EXCEPTION 'Both incidentinmate and incident_inmate exist; reconcile manually before migration';
    END IF;
END;
$$;
CREATE TABLE IF NOT EXISTS cells (
    id BIGSERIAL PRIMARY KEY,
    block_name VARCHAR(50) NOT NULL,
    cell_number INTEGER NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    classification VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_cells_block_number UNIQUE (block_name, cell_number)
);

CREATE TABLE IF NOT EXISTS cell_allocations (
    id BIGSERIAL PRIMARY KEY,
    inmate_id BIGINT NOT NULL UNIQUE,
    cell_id BIGINT NOT NULL,
    allocated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cell_allocations_inmate
        FOREIGN KEY (inmate_id) REFERENCES inmates (id) ON DELETE CASCADE,
    CONSTRAINT fk_cell_allocations_cell
        FOREIGN KEY (cell_id) REFERENCES cells (id) ON DELETE CASCADE
);

INSERT INTO cells (block_name, cell_number, capacity, classification) VALUES
    ('A', 1, 2, 'Minimum'),
    ('A', 2, 2, 'Medium'),
    ('B', 1, 3, 'Maximum')
ON CONFLICT (block_name, cell_number) DO NOTHING;

