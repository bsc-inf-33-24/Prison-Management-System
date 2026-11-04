ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (
    'Super Admin', 'Records Officer', 'Disciplinary Officer', 'Case Officer',
    'Visitor Officer', 'Rehabilitation Officer', 'Instructor', 'Medical Officer', 'Duty Officer'
));

ALTER TABLE rehabprogram ADD COLUMN instructor_user_id BIGINT NULL;
ALTER TABLE rehabprogram ADD CONSTRAINT fk_rehabprogram_instructor_user
    FOREIGN KEY (instructor_user_id) REFERENCES users (user_id) ON DELETE SET NULL;
CREATE INDEX idx_rehabprogram_instructor_user ON rehabprogram (instructor_user_id);
