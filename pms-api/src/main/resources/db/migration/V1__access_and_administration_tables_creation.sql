

CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (
        role IN (
            'Super Admin',
            'Records Officer',
            'Disciplinary Officer',
            'Case Officer',
            'Visitor Officer',
            'Rehabilitation Officer',
            'Medical Officer',
            'Duty Officer'
        )
    ),
    status VARCHAR(16) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE user_permissions (
    user_id BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    permission VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, permission)
);
