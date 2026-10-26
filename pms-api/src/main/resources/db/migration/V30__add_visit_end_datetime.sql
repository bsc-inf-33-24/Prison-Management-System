ALTER TABLE visits
    ADD COLUMN IF NOT EXISTS visit_end_datetime TIMESTAMPTZ;

UPDATE visits
SET visit_end_datetime = visit_datetime + duration_minutes * INTERVAL '1 minute'
WHERE visit_end_datetime IS NULL;

ALTER TABLE visits
    ALTER COLUMN visit_end_datetime SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_visits_end_after_start'
          AND conrelid = 'visits'::regclass
    ) THEN
        ALTER TABLE visits
            ADD CONSTRAINT ck_visits_end_after_start
            CHECK (visit_end_datetime >= visit_datetime);
    END IF;
END;
$$;
