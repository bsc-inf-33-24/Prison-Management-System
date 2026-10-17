# Medical Officer API test guide

Create `medical01` with role `Medical Officer` and log in. The current API defines this role, but no controller endpoint grants it role-specific access. There are no medical-record endpoints implemented in the current controllers.

For now, use this account to verify role restrictions: a valid Medical Officer token should receive `403` on role-protected inmate, cell, case, incident, visitor, and rehabilitation endpoints. Authentication endpoints such as `POST /api/v1/auth/password/change` are available to an authenticated user:

```json
{"currentPassword":"Medical123!","newPassword":"NewMedical123!"}
```

If medical endpoints are added, this guide can be extended with their request bodies and positive tests.
