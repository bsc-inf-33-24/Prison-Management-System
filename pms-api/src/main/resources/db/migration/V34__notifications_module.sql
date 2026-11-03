ALTER TABLE notification
    ADD COLUMN title VARCHAR(255);

UPDATE notification
SET title = COALESCE(NULLIF(type, ''), 'Notification')
WHERE title IS NULL;

UPDATE notification
SET created_at = NOW()
WHERE created_at IS NULL;

ALTER TABLE notification
    ALTER COLUMN title SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN is_read TYPE BOOLEAN
        USING CASE
            WHEN lower(COALESCE(is_read, 'false')) IN ('true', 't', '1', 'yes', 'y', 'read') THEN TRUE
            ELSE FALSE
        END,
    ALTER COLUMN is_read SET DEFAULT FALSE,
    ALTER COLUMN is_read SET NOT NULL,
    ADD COLUMN read_at TIMESTAMPTZ;

CREATE INDEX idx_notification_recipient_unread_created
    ON notification (recipient_user_id, is_read, created_at DESC);
