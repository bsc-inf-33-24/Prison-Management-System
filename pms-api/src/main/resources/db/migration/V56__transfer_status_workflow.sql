ALTER TABLE inmate_transfers ALTER COLUMN destination_prison_id DROP NOT NULL;
ALTER TABLE inmate_transfers DROP CONSTRAINT IF EXISTS inmate_transfers_status_check;
ALTER TABLE inmate_transfers ADD CONSTRAINT inmate_transfers_status_check
    CHECK (status IN ('PENDING','APPROVED','REJECTED','CONFIRMED','IN_TRANSIT','RECEIVED','CANCELLED'));
