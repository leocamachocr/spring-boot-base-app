# Research: Migrate Codebase to the Reference Architecture

## R1. Invalid login credentials

- **Finding**: `LoginUserHandlerImpl` calls `AuthenticationManager.authenticate`, which throws
  `BadCredentialsException` on wrong credentials. `Result.InvalidCredentials` is never returned, and the controller
  branch for it would call `BaseException.exceptionBuilder().build()` without a code (NPE on `code.message()`).
  Today the exception reaches the `Throwable` handler → 400 `UNKNOWN_ERROR`.
- **Decision**: catch `AuthenticationException` in the handler and return `InvalidCredentials`; controller maps it
  to 400 with new `ErrorCode.INVALID_CREDENTIALS(2003)`.
- **Rationale**: constitution III (expected outcomes are Results); issue #5 point 4 lists invalid credentials
  explicitly.
- **Alternatives**: keep `UNKNOWN_ERROR` (rejected: violates III); 401 status (rejected: contract change beyond
  what the issue asks).

## R2. UUID identifiers

- **Finding**: changeset 1 already creates `users.id UUID PRIMARY KEY`; the entity assigns `UUID.randomUUID()` in
  code.
- **Decision**: `@GeneratedValue(strategy = GenerationType.UUID)` and stop assigning ids manually. No new
  changeset.
- **Rationale**: reference §6; schema already compatible, and constitution forbids editing existing changesets —
  nothing to migrate.
- **Alternatives**: new changeset (rejected: no schema difference to express).

## R3. Controller return types vs. ArchUnit

- **Finding**: current rule requires public controller methods to return `api.types`. Error outcomes need a status
  and an `ErrorResponse` body without throwing.
- **Decision**: controllers return `ResponseEntity<?>` (reference §4); rule allows `api.responses` or
  `http.ResponseEntity`.
- **Alternatives**: keep throwing an exception for expected outcomes (rejected: III); `@ResponseStatus` per result
  (rejected: cannot vary per branch).

## R4. Status codes for expected outcomes

- **Decision**: keep 400 for all expected outcomes, as today.
- **Rationale**: FR-002 (contract preserved). More precise codes (409, 404, 401) are a separate product decision.

## R5. Unexpected errors

- **Decision**: `GlobalExceptionHandler` handles `BusinessException` → 500 (reference §7) and keeps a `Throwable`
  fallback → 400 `UNKNOWN_ERROR` (current behavior, e.g. malformed JSON).
- **Rationale**: one `@RestControllerAdvice` (III); no regression for clients relying on today's fallback.

## R6. ArchUnit layer patterns

- **Finding**: `..api..` would also match `dev.leocamacho.demo.tests.api..` (Cucumber glue), putting tests into
  the `api` layer.
- **Decision**: define layers with fully qualified roots (`dev.leocamacho.demo.api..` etc.).

## R7. Where things without a reference slot go

- `AuthenticatedUser` (Spring `UserDetails`) → `security` (only security and the login handler use it).
- `GlobalExceptionHandler` → root of `api` (reference §7: "in `api`").
- `providers` → not created (FR-010, constitution VII).
- Handler names and `impl` sub-packages → kept (spec assumptions).
