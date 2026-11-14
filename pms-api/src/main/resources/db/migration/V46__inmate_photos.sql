CREATE TABLE inmate_photos (
    id BIGSERIAL PRIMARY KEY,
    public_uuid UUID NOT NULL UNIQUE,
    inmate_id BIGINT NOT NULL UNIQUE REFERENCES inmates(id) ON DELETE CASCADE,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    content_type VARCHAR(50) NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png')),
    file_size BIGINT NOT NULL CHECK (file_size BETWEEN 1 AND 5242880),
    uploaded_by_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
