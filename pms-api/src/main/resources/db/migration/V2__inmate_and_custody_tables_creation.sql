
CREATE TABLE Inmate (
    inmate_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    gender VARCHAR(10),
    date_of_birth DATE,
    national_id VARCHAR(255),
    admission_date DATE,
    photo BYTEA,
    next_of_kin VARCHAR(255),
    classification VARCHAR(255),
    status VARCHAR(255)
);

CREATE TABLE Cell (
    cell_id BIGSERIAL PRIMARY KEY,
    cell_number INTEGER NOT NULL,
    block INTEGER NOT NULL,
    capacity INTEGER,
    current_occupancy VARCHAR(255),
    classification_type VARCHAR(255)
);

CREATE TABLE CellAssignment (
    assignment_id BIGSERIAL PRIMARY KEY,
    inmate_id BIGINT REFERENCES Inmate (inmate_id) ON UPDATE CASCADE ON DELETE SET NULL,
    cell_id BIGINT REFERENCES Cell (cell_id) ON UPDATE CASCADE ON DELETE SET NULL,
    date_assigned DATE,
    date_released DATE,
    assigned_by VARCHAR(255)
);
