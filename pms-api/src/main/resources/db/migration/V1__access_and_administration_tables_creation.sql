

create table Role(
    role_id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(255) NOT NULL,
    permissions TEXT[] NOT NULL 
);

create table User(
    user_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(100),
    password_hash TEXT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role_id BIGINT REFERENCES Role(role_id) ON UPDATE CASCADE ON DELETE SET NULL,
    status VARCHAR(50) CHECK (status IN ('active','deactivated')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

create table Tokens(
    tokens TEXT PRIMARY KEY,
    user_id BIGINT REFERENCES User(user_id) ON DELETE CASCADE NOT NULL
);

