# Data Model: Migrate Codebase to the Reference Architecture

## Persistence

### `persistence.model.UserEntity` (table `users`, unchanged schema)

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = GenerationType.UUID)`; no longer assigned in code |
| `name` | `String` | required (builder validation kept) |
| `email` | `String` | required, used as login username |
| `password` | `String` | BCrypt hash |

## Handler contracts (no entities)

| Handler | Input | Result variants |
|---|---|---|
| `RegisterUserHandler` | `Command(name, email, password)` | `Success(UUID id)`, `InvalidFields(String... fields)`, `EmailAlreadyExists()` |
| `LoginUserHandler` | `Command(email, password)` | `Success(token, name, email)`, `InvalidCredentials()` |
| `EncodePasswordHandler` | `Command(password)` | `Success(encodedPassword)` |
| `GetCurrentUserQuery` | `query(String email)` | `Success(UUID id, String name, String email)`, `UserNotFound()` |

## Error codes (`exception.ErrorCode`)

| Code | Name | Message | Change |
|---|---|---|---|
| 1001 | `ERROR_NOT_IDENTIFIED` | Error not identified | — |
| 1002 | `REQUIRED_FIELDS` | Required fields are missing | — |
| 1003 | `UNKNOWN_ERROR` | Unknown error | — |
| 401 | `UNAUTHORIZED` | Unauthorized | — |
| 2001 | `INVALID_USER` | Invalid User | — |
| 2002 | `EMAIL_ALREADY_EXISTS` | Email already exists | — |
| 2003 | `INVALID_CREDENTIALS` | Invalid credentials | **new** |
