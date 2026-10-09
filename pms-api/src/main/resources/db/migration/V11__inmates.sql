CREATE TABLE inmates (
    id BIGSERIAL PRIMARY KEY,
    inmate_number VARCHAR(30) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    admission_date DATE NOT NULL,
    classification VARCHAR(50) NOT NULL,
    current_facility VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_inmates_inmate_number UNIQUE (inmate_number)
);
