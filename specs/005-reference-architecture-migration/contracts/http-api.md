# HTTP Contract (preserved)

Error body for every non-2xx response below:

```json
{ "message": "<ErrorCode.message>", "code": <ErrorCode.code>, "correlationId": "<uuid>", "params": ["..."] }
```

## `POST /api/public/register`

Request: `{ "user": "...", "email": "...", "password": "..." }`

| Case | Status | Body |
|---|---|---|
| Registered | 200 | `{ "result": "<user uuid>" }` |
| Missing fields | 400 | code 1002, `params` = missing field names (`user`, `username`, `password`) |
| Email exists | 400 | code 2002 |

## `POST /api/public/login`

Request: `{ "username": "<email>", "password": "..." }`

| Case | Status | Body |
|---|---|---|
| Valid | 200 | `{ "token": "...", "name": "...", "email": "..." }` |
| Invalid credentials | 400 | **code 2003 "Invalid credentials"** (was 1003 "Bad credentials") |

## `GET /api/private/users/current` (Bearer token)

| Case | Status | Body |
|---|---|---|
| Found | 200 | `{ "name": "...", "email": "..." }` |
| User not found | 400 | code 2001 |
| No/invalid token | 401 | code 401 "Unauthorized" (security entry point, unchanged) |
