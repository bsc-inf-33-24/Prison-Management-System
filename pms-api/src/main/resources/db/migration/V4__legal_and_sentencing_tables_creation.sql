
CREATE TABLE "Case" (
    case_id BIGSERIAL PRIMARY KEY,
    inmate_id BIGINT REFERENCES Inmate (inmate_id) ON UPDATE CASCADE ON DELETE SET NULL,
    case_number VARCHAR(100),
    court VARCHAR(255),
    case_start_date DATE,
    case_end_date DATE,
     status VARCHAR(50) CHECK (status IN ('ongoing', 'closed', 'appealed')),
    recorded_by VARCHAR(255)
);

CREATE TABLE Crime (
    crime_id BIGSERIAL PRIMARY KEY,
    offence_name VARCHAR(255),
    description TEXT,
    category VARCHAR(255)
);

CREATE TABLE CaseCrime (
    case_id BIGINT REFERENCES "Case" (case_id) ON DELETE CASCADE,
    crime_id BIGINT REFERENCES Crime (crime_id) ON DELETE CASCADE,

    PRIMARY KEY (case_id, crime_id)
);

CREATE TABLE Sentence (
    sentence_id BIGSERIAL PRIMARY KEY,
    case_id BIGINT REFERENCES "Case" (case_id) ON UPDATE CASCADE ON DELETE SET NULL,
    inmate_id BIGINT REFERENCES Inmate (inmate_id) ON UPDATE CASCADE ON DELETE SET NULL,
    duration INTEGER,
    start_date DATE,
    release_date DATE,
    status VARCHAR(50) CHECK (status IN ('active', 'completed', 'commuted'))
);
