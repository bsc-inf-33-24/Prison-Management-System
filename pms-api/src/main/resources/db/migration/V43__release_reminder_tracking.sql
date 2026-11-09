ALTER TABLE inmates ADD COLUMN release_reminder_sent_at TIMESTAMPTZ;
CREATE INDEX idx_inmates_projected_release_date
    ON inmates(projected_release_date)
    WHERE projected_release_date IS NOT NULL AND release_reminder_sent_at IS NULL;
