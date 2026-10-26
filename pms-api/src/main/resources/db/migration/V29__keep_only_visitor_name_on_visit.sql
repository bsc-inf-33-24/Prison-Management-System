ALTER TABLE visits
    DROP COLUMN IF EXISTS visitor_phone,
    DROP COLUMN IF EXISTS visitor_relationship_to_inmate;
