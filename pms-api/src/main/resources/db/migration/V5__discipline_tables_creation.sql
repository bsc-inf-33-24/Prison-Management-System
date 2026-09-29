
create table Incident(
    incident_id BIGSERIAL PRIMARY KEY,
    incident_date DATE,
    description TEXT,
    status VARCHAR(50) CHECK (status IN ('resolved','open')),
    outcome VARCHAR(255),
    reported_by VARCHAR(255)
)

create table IncidentInmate(
    incident_id BIGSERIAL REFERENCES Incident(incident_id) ON DELETE CASCADE,
    inmate_id BIGINT REFERENCES Inmate(inmate_id) ON DELETE CASCADE,
    role_in_incident VARCHAR(255),

    PRIMARY KEY (incident_id, inmate_id)
)

