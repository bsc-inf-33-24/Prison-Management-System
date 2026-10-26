ALTER TABLE visits
    ADD COLUMN IF NOT EXISTS visitor_full_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS visitor_id_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS visitor_phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS visitor_relationship_to_inmate VARCHAR(255);

UPDATE visits v
SET visitor_full_name = w.full_name,
    visitor_id_number = w.id_number,
    visitor_phone = w.phone,
    visitor_relationship_to_inmate = w.relationship_to_inmate
FROM visitors w
WHERE v.visitor_id = w.visitor_id
  AND (
      v.visitor_full_name IS NULL
      OR v.visitor_id_number IS NULL
      OR v.visitor_relationship_to_inmate IS NULL
  );

ALTER TABLE visits
    ALTER COLUMN visitor_full_name SET NOT NULL,
    ALTER COLUMN visitor_id_number SET NOT NULL,
    ALTER COLUMN visitor_relationship_to_inmate SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_visits_visitor_full_name ON visits (visitor_full_name);
CREATE INDEX IF NOT EXISTS idx_visits_visitor_id_number ON visits (visitor_id_number);

ALTER TABLE visits DROP CONSTRAINT IF EXISTS visits_visitor_id_fkey;
DROP INDEX IF EXISTS idx_visits_visitor_id;
ALTER TABLE visits DROP COLUMN IF EXISTS visitor_id;

DROP INDEX IF EXISTS uk_visitors_id_number;
DROP TABLE IF EXISTS visitors;
