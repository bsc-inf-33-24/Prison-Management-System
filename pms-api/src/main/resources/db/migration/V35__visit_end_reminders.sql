ALTER TABLE visits
    ADD COLUMN end_reminder_sent_at TIMESTAMPTZ;

CREATE INDEX idx_visits_end_reminder_due
    ON visits (visit_end_datetime)
    WHERE end_reminder_sent_at IS NULL;
