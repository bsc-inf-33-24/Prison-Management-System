-- The rehabilitation model now uses the current `inmates` table. V7's FK
-- points at the legacy singular `Inmate` table, while InmateRepository reads
-- from `inmates`; replace the target without rewriting existing enrollment IDs.
ALTER TABLE enrollment
    DROP CONSTRAINT IF EXISTS enrollment_inmate_id_fkey;

-- Existing rows may still reference legacy Inmate IDs. NOT VALID preserves
-- those rows while PostgreSQL enforces this relationship for new writes.
ALTER TABLE enrollment
    ADD CONSTRAINT enrollment_inmate_id_fkey
    FOREIGN KEY (inmate_id) REFERENCES inmates (id)
    ON UPDATE CASCADE ON DELETE SET NULL
    NOT VALID;
