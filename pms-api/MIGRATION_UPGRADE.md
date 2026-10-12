# Flyway migration upgrade procedure

## Migration order and dependencies

The versions are unique and intentionally ordered as follows:

| Version | Migration | Dependency or reason |
| --- | --- | --- |
| V12 | Rename `incidentinmate` to `incident_inmate` | V5 creates the legacy table. The guarded rename can run on a pre-V12 baseline and safely does nothing when already renamed. |
| V13 | Cells and allocations | `cell_allocations.inmate_id` references V11 `inmates(id)`. It repeats the guarded incident rename for databases where a different V12 already ran. |
| V14 | Case management tables | `cases.inmate_id` references V11 `inmates(id)` and `created_by` references V1 `users(user_id)`. |

Cell and case migrations have no dependency on each other. The selected order is stable. Existing legacy `Case`, `Crime`, `CaseCrime`, and `Sentence` tables are left in place; these migrations never drop or truncate them.

## Determine database history before upgrade

The repository does not identify deployed/shared database instances or their `flyway_schema_history` contents. Do not assume which former V12 file ran. Before upgrading each existing database, take a restorable backup and inspect its schema history and table state:

```sql
SELECT installed_rank, version, description, script, checksum, success
FROM flyway_schema_history
ORDER BY installed_rank;

SELECT to_regclass('public.incidentinmate') AS old_incident_table,
       to_regclass('public.incident_inmate') AS current_incident_table,
       to_regclass('public.cells') AS cells,
       to_regclass('public.cell_allocations') AS allocations,
       to_regclass('public.cases') AS cases,
       to_regclass('public.crimes') AS crimes,
       to_regclass('public.case_crimes') AS case_crimes;
```

Also inspect row counts and constraints on any existing current or legacy tables. If both incident table names exist, stop for manual reconciliation; V12/V13 deliberately fail rather than guessing which data to retain.

## Clean database or verified pre-V12 baseline

With the normal application environment configured (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`), start the application. Flyway applies V1 through V14 before Hibernate validates the schema. For the checked-in Docker Compose setup, set `DB_PASSWORD` and `JWT_SECRET` in `.env`, then run `docker compose up --build`.

For an existing database at a verified V1–V11 baseline, deploy this version and let Flyway apply V12–V14. V13's `CREATE TABLE IF NOT EXISTS` and conflict-safe seed rows accommodate a database where a former V12 already created the cells tables; V14 similarly accommodates the former case migration having created its current tables.

## Database that already recorded a former V12

The former repository had three different scripts named V12, so an existing history record may have a different script name or checksum than the now-canonical V12. Never run `repair` based only on the version number. Compare the recorded V12 script/checksum with the former release artifact and verify the actual schema against that script. Confirm a restorable backup first.

If the history is proven to represent one of the former V12 scripts and its effects are present, a DBA may perform a one-time Flyway `repair` with the new migration artifact to align the V12 checksum, then run normal migration. V13 covers a missing incident rename; V13/V14 are idempotent for the former cells/case tables. If evidence is incomplete or the schema differs from the recorded migration, stop and prepare a reviewed, data-preserving reconciliation migration for that specific database. Do not delete history rows, baseline over the discrepancy, or reset the schema.

## Verification

Run the version uniqueness check with:

```powershell
./scripts/check-flyway-versions.ps1
```

Against an isolated PostgreSQL database, run Flyway validate and migrate once from empty and once from a representative V1–V11 snapshot. Then inspect `flyway_schema_history`, tables, columns, indexes, foreign keys, and constraints, and start the app with `spring.jpa.hibernate.ddl-auto=validate`. Do not point this verification at a shared or production database.
