-- V5 created this table as unquoted "IncidentInmate", which PostgreSQL folds
-- to lowercase only (incidentinmate - no underscore). Hibernate's default
-- naming strategy maps the IncidentInmate entity's @Table(name="IncidentInmate")
-- to snake_case (incident_inmate), so schema validation fails looking for a
-- table that was never actually created under that name. Rename to match.
ALTER TABLE IncidentInmate RENAME TO incident_inmate;
