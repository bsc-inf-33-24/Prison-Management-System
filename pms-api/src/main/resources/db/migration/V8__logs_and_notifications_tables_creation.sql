CREATE TABLE AuditLog (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action_type VARCHAR(255),
    entity_name VARCHAR(255),
    record_id VARCHAR(50),
    old_values TEXT,
    new_values TEXT,
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE Notification (
    notification_id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT REFERENCES users (user_id) ON UPDATE CASCADE ON DELETE SET NULL,
    type VARCHAR(255),
    message TEXT,
    related_record_id VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    is_read VARCHAR(10)
);