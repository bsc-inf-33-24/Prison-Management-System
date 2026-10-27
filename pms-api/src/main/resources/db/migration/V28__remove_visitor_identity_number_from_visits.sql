DROP INDEX IF EXISTS idx_visits_visitor_id_number;
ALTER TABLE visits DROP COLUMN IF EXISTS visitor_id_number;
