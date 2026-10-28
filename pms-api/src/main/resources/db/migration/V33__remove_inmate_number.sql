ALTER TABLE inmates DROP CONSTRAINT IF EXISTS uk_inmates_inmate_number;
ALTER TABLE inmates DROP COLUMN IF EXISTS inmate_number;
