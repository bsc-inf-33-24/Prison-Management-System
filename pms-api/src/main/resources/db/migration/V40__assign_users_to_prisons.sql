ALTER TABLE users ADD COLUMN prison_id BIGINT REFERENCES prisons(id);
CREATE INDEX idx_users_prison_id ON users(prison_id);
