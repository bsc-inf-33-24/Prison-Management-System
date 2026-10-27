-- The original incident table referenced the legacy singular `inmate` table.
-- The API now uses the current `inmates` table. NOT VALID preserves any
-- historical links that cannot be migrated automatically while enforcing
-- the corrected relationship for all new and updated rows.
ALTER TABLE incident_inmate
    DROP CONSTRAINT IF EXISTS incidentinmate_inmate_id_fkey;

ALTER TABLE incident_inmate
    ADD CONSTRAINT fk_incident_inmate_current_inmates
    FOREIGN KEY (inmate_id) REFERENCES inmates (id) ON DELETE CASCADE NOT VALID;
