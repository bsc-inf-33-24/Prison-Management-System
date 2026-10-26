-- Inmate enrollment no longer stores a current facility. The original column
-- remained NOT NULL after the entity stopped mapping it, causing every insert
-- to fail with a database integrity violation.
ALTER TABLE inmates DROP COLUMN current_facility;
