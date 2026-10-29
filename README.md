# Prison Management System documentation

This repository stores the documentation and project assets for the Prison Management System. The application code lives in the `pms-api/` subdirectory.

## Documentation index

- [pms-api/README.md](./pms-api/README.md) — application overview, local startup notes, and API guide links
- [pms-api/docs/api-reference.md](./pms-api/docs/api-reference.md) — the authoritative REST API reference for the current implementation
- [pms-api/docs/testing/api-testing-user-guide.md](./pms-api/docs/testing/api-testing-user-guide.md) — historical manual testing notes; verify each route against the API reference because this file contains stale examples

## Important note

This repository is intentionally documentation-focused. The implementation source of truth is the Java application under `pms-api/`, and the actual API contract is derived from the current controllers, DTOs, security configuration, and runtime validation logic.
