create table AuditLog(
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action_type VARCHAR(255),
    entity_name VARCHAR(255),
    record_id VARCHAR(50),
    old/new_values VARCHAR(255),
    timestamp TIMESTAMPTZ DEFAULT NOW()
);

create table Notification(
    notification_id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT REFERENCES User(user_id) ON UPDATE CASCADE ON DELETE SET NULL,
    type VARCHAR(255),
    message TEXT,
    related_record_id VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    is_read VARCHAR(10)
);