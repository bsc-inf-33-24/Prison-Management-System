# Duty Officer API test guide

Create `duty01` with role `Duty Officer`, log in, and use `accessToken` as `TOKEN_DUTY`. The current API defines Duty Officer, but no business endpoint currently grants it role-specific access.

Use this account to check authorization: it should be able to authenticate and change its password, but should receive `403` on role-protected inmate, cell, case, incident, visitor, and rehabilitation endpoints.

`POST /api/v1/auth/password/change`:

```json
{"currentPassword":"Duty123!","newPassword":"NewDuty123!"}
```
