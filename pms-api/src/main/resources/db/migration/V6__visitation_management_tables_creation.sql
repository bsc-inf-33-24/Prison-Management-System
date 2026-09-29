
create table Visitor(
    visitor_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    id_number VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    relationship_to_inmate VARCHAR(255) NOT NULL
);

create table Visit(
    visit_id BIGSERIAL PRIMARY KEY,
    visitor_id BIGINT REFERENCES Visitor(visit_id) ON UPDATE CASCADE ON DELETE SET NULL,
    visit_datetime DATE NOT NULL,
    duration NUMBER,
    logged_by VARCHAR(255) NOT NULL
);
