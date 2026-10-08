
CREATE TABLE Visitor (
    visitor_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    id_number VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    relationship_to_inmate VARCHAR(255) NOT NULL
);

CREATE TABLE Visit (
    visit_id BIGSERIAL PRIMARY KEY,
    visitor_id BIGINT REFERENCES Visitor (visitor_id) ON UPDATE CASCADE ON DELETE SET NULL,
    visit_datetime DATE NOT NULL,
    duration INTEGER,
    logged_by VARCHAR(255) NOT NULL
);
