CREATE TABLE prisons (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    location VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE blocks (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    prison_id BIGINT NOT NULL REFERENCES prisons(id),
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_blocks_prison_name UNIQUE (prison_id, name)
);

ALTER TABLE cells ADD COLUMN block_id BIGINT REFERENCES blocks(id);
ALTER TABLE inmates ADD COLUMN current_prison_id BIGINT REFERENCES prisons(id);

ALTER TABLE cells DROP CONSTRAINT IF EXISTS uk_cells_block_number;
CREATE UNIQUE INDEX uk_cells_block_number ON cells (block_id, cell_number) WHERE block_id IS NOT NULL;
CREATE INDEX idx_cells_block_id ON cells(block_id);
CREATE INDEX idx_inmates_current_prison ON inmates(current_prison_id);
