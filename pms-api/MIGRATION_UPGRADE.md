# Flyway migration upgrade procedure

## Migration order and dependencies

| Version | Migration | Dependency or reason |
| --- | --- | --- |
| V12 | Rename `incidentinmate` to `incident_inmate` | V5 creates the legacy table. This original migration is retained unchanged for checksum compatibility. |
| V13 | Cells and allocations | `cell_allocations.inmate_id` references V11 `inmates(id)`. It repeats a guarded incident rename for databases where a different V12 already ran. |
| V14 | Case management and current sentences | `cases.inmate_id` references V11 `inmates(id)` and `created_by` references V1 `users(user_id)`; `sentences` references the current `cases` and V11 `inmates`. |

Cell and case migrations do not depend on each other. Existing legacy `Case`, `crime`, `casecrime`, and singular `sentence` tables are retained. V14 creates the current plural `sentences` table. Legacy rows remain in the legacy tables and are not copied into the current case/sentence model. No migration drops or truncates these tables.

## Determine database history before upgrade

The repository does not include an inventory or credentials for deployed/shared databases, so whether any have recorded a former V12 is unverified. Do not assume which former V12 file ran. Before upgrading each existing database, take a restorable backup and inspect its schema history and table state:

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
       to_regclass('public.case_crimes') AS case_crimes,
       to_regclass('public.sentence') AS legacy_sentence,
       to_regclass('public.sentences') AS current_sentences;
```

Also inspect row counts and constraints on current and legacy tables. If both incident table names exist, stop for manual reconciliation; V13 fails rather than guessing which data to retain.

## Clean database or verified pre-V12 baseline

Configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`, then start the application. Flyway applies V1 through V14 before Hibernate validates the schema. For the checked-in Docker Compose setup, set `DB_PASSWORD` and `JWT_SECRET` in `.env`, then run `docker compose up --build`.

For an existing database verified at V1–V11, deploy this version and let Flyway apply V12–V14. V13 safely handles cells that a former V12 may already have created; V14 does the same for the former current case tables and preserves old legacy tables.

## Database that already recorded a former V12

The former repository had three different scripts named V12. An existing history record may therefore have a different script name or checksum than the canonical V12. Never run `repair` based only on the version number. Compare the recorded V12 script/checksum with the former release artifact and verify the actual schema against that script. Confirm a restorable backup first.

If history is proven to represent one of the former V12 scripts and its effects are present, a DBA may perform a one-time Flyway `repair` with the new migration artifact to align V12, then run normal migration. V13 covers a missing incident rename and avoids recreating existing cells. V14 avoids recreating existing current case tables. If evidence is incomplete or the schema differs from the recorded migration, stop and prepare a reviewed, data-preserving reconciliation for that database. Do not delete history rows, baseline over the discrepancy, or reset the schema.

## Verification

Run the version uniqueness check with:

```powershell
./scripts/check-flyway-versions.ps1
```

CI runs that check, an empty-database application startup with Hibernate validation, and a Testcontainers migration from V1–V11. The pre-V12 test validates the baseline, migrates through V14, validates the final history, checks tables, columns, unique indexes, and foreign keys, and confirms seeded legacy case and sentence rows remain. Do not point these checks at a shared or production database.
