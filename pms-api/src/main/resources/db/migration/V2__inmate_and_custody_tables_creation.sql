
create table Inmate(
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

create table Cell(
    cell_id BIGSERIAL PRIMARY KEY,
    cell_number NUMBER NOT NULL,
    block NUMBER NOT NULL,
    capacity NUMBER,
    current_occupancy VARCHAR(255),
    classification_type VARCHAR(255)
);

create table CellAssignment(
    assignment_id BIGSERIAL PRIMARY KEY,
    inmate_id REFERENCES Inmate(inmate_id) ON UPDATE CASCADE ON DELETE SET NULL,
    cell_id REFERENCES Cell(cell_id) ON UPDATE CASCADE ON DELETE SET NULL,
    date_assigned DATE,
    date_released DATE,
    assigned_by VARCHAR(255)
);
