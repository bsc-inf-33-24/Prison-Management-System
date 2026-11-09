ALTER TABLE cell_allocations ADD COLUMN IF NOT EXISTS released_at TIMESTAMPTZ;
ALTER TABLE cell_allocations DROP CONSTRAINT IF EXISTS cell_allocations_inmate_id_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cell_allocations_active_inmate ON cell_allocations(inmate_id) WHERE released_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_cell_allocations_active_cell ON cell_allocations(cell_id) WHERE released_at IS NULL;

DO $$
DECLARE
    legacy_column_count INTEGER;
    legacy_sequence TEXT;
BEGIN
    IF to_regclass('public.inmate_transfers') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'inmate_transfers'
             AND column_name = 'inmate_id'
       ) THEN
        SELECT COUNT(*)
        INTO legacy_column_count
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'inmate_transfers'
          AND column_name IN (
              'transfer_uuid',
              'source_prison_uuid',
              'destination_prison_uuid',
              'source_inmate_uuid',
              'destination_inmate_uuid'
          );

        IF legacy_column_count <> 5 THEN
            RAISE EXCEPTION 'Existing public.inmate_transfers table has an unsupported schema';
        END IF;

        IF to_regclass('public.inmate_transfers_legacy') IS NOT NULL THEN
            RAISE EXCEPTION 'Cannot preserve existing transfers: public.inmate_transfers_legacy already exists';
        END IF;

        legacy_sequence := pg_get_serial_sequence('public.inmate_transfers', 'id');
        ALTER TABLE public.inmate_transfers RENAME TO inmate_transfers_legacy;

        IF legacy_sequence IS NOT NULL THEN
            EXECUTE format(
                'ALTER SEQUENCE %s RENAME TO %I',
                legacy_sequence,
                'inmate_transfers_legacy_id_seq'
            );
        END IF;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS inmate_transfers (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    inmate_id BIGINT NOT NULL REFERENCES inmates(id),
    source_prison_id BIGINT NOT NULL REFERENCES prisons(id),
    destination_prison_id BIGINT NOT NULL REFERENCES prisons(id),
    reason VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','IN_TRANSIT','RECEIVED','CANCELLED')),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    requested_by VARCHAR(100) NOT NULL,
    decided_at TIMESTAMPTZ,
    decided_by VARCHAR(100),
    decision_note VARCHAR(2000),
    dispatched_at TIMESTAMPTZ,
    dispatched_by VARCHAR(100),
    received_at TIMESTAMPTZ,
    received_by VARCHAR(100),
    cancelled_at TIMESTAMPTZ,
    cancelled_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_transfer_distinct_prisons CHECK (source_prison_id <> destination_prison_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_active_transfer_per_inmate ON inmate_transfers(inmate_id)
    WHERE status IN ('PENDING','APPROVED','IN_TRANSIT');
CREATE INDEX IF NOT EXISTS idx_transfers_source_requested ON inmate_transfers(source_prison_id, requested_at DESC);
CREATE INDEX IF NOT EXISTS idx_transfers_destination_requested ON inmate_transfers(destination_prison_id, requested_at DESC);
