
CREATE TABLE RehabProgram (
    program_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    instructor VARCHAR(255) NOT NULL,
    schedule VARCHAR(255),
    status VARCHAR(255)
);

CREATE TABLE Enrollment (
    enrollment_id BIGSERIAL PRIMARY KEY,
    inmate_id BIGINT REFERENCES Inmate (inmate_id) ON UPDATE CASCADE ON DELETE SET NULL,
    program_id BIGINT REFERENCES RehabProgram (program_id) ON DELETE SET NULL,
    enrollment_date DATE,
    status VARCHAR(255)
);

CREATE TABLE AttendanceRecord (
    attendance_id BIGSERIAL PRIMARY KEY,
    enrollment_id BIGINT REFERENCES Enrollment (enrollment_id) ON UPDATE CASCADE ON DELETE SET NULL,
    session_date DATE,
    present VARCHAR(10) NOT NULL CHECK (present IN ('Y', 'N')),
    remarks TEXT,
    recorded_by VARCHAR(255) NOT NULL
);
