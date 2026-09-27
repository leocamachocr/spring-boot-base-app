# Implementation Plan: Migrate Codebase to the Reference Architecture

**Branch**: `speckit/005-reference-architecture-migration` | **Date**: 2026-09-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/005-reference-architecture-migration/spec.md`

## Summary

Move the existing code (users: register, login, current user) into the package layout of `docs/ARCHITECTURE.md`
(§2), make handler results independent of JPA entities (§3), translate every expected outcome to HTTP in the
controllers (§4) and replace `BaseException` + `ExceptionManagerController` with an unchecked `BusinessException`
reserved for rollback plus a single `GlobalExceptionHandler` (§7). The HTTP contract is preserved; the only
intentional change is that invalid login credentials become an explicit `Result` with a dedicated error code
(today they escape as a generic `UNKNOWN_ERROR`). ArchUnit rules are rewritten for the new layout and gain a rule
that forbids entities in handler contracts.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 4.0.2 (Web MVC, Security, Data JPA), jjwt 0.12.6, Liquibase
**Storage**: H2 file DB; table `users` already has `id UUID PRIMARY KEY` (changeset 1)
**Testing**: JUnit 5 + Mockito (unit), ArchUnit 1.3.0 (structure), Cucumber 7 + REST-Assured 6 (e2e, `e2eTest`)
**Target Platform**: JVM web service
**Project Type**: web-service (single Gradle project)
**Performance Goals**: N/A (structural refactor, no runtime change)
**Constraints**: HTTP contract unchanged (FR-001/FR-002); no new dependencies; existing changesets not edited;
Checkstyle (Sun, max 120) green
**Scale/Scope**: ~25 main classes, ~15 test classes; 3 endpoints

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Reference Architecture | PASS | This feature *implements* it: packages per §2, explicit mapping per §3, providers omitted because no external integration exists (§5, FR-010), UUID via `@GeneratedValue(UUID)` per §6. |
| II. CQRS Use Cases | PASS | Interfaces keep nested `Command`/`Query` + sealed `Result`; `Result`s get their own records (no `UserEntity`); controllers only translate. |
| III. Results first, exceptions for rollback | PASS | `InvalidCredentials` becomes reachable (handler catches `AuthenticationException`); controllers no longer throw for expected outcomes; `BusinessException` + one `@RestControllerAdvice`. |
| IV. Enforced Layering | PASS | ArchUnit rules are *changed deliberately* in this PR (new package names, no `@Repository`, new entity-free-contract rule); none is removed or weakened. Reason stated in PR. |
| V. Tests accompany change | PASS | Updated `RegisterUserHandlerTests`; new unit tests for `LoginUserHandlerImpl` and `GetCurrentUserQueryImpl`; new Cucumber scenario for invalid credentials (not executed in build). |
| VI. Green build | PASS (gate for implementation) | `./gradlew build` must pass. |
| VII. Simplicity | PASS | Handler names and `impl` sub-packages kept; no Bean Validation starter; no `providers`; no DB migration. |

Transition section: this is issue #5 itself, so it is allowed. Removing the Transition section from the
constitution is a follow-up for a human (the workflow may not edit `.specify/`).

**Post-design re-check**: PASS — design below introduces no deviations; Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/005-reference-architecture-migration/
├── plan.md              # This file
├── research.md          # Phase 0 decisions
├── data-model.md        # Phase 1: entity + Result records + error codes
├── quickstart.md        # Phase 1: how to verify the migration
├── contracts/
│   └── http-api.md      # Phase 1: preserved HTTP contract (+ invalid credentials)
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
src/main/java/dev/leocamacho/demo/
├── App.java
├── api/
│   ├── GlobalExceptionHandler.java        # was api/exceptions/ExceptionManagerController
│   ├── controllers/                       # was api/rests
│   │   ├── LoginUserController.java
│   │   ├── RegisterUserController.java
│   │   └── UserQueriesController.java
│   ├── request/                           # was api/types (inputs)
│   │   ├── LoginUserRequest.java
│   │   └── RegisterUserRequest.java
│   └── responses/                         # was api/types (outputs)
│       ├── ErrorResponse.java             # + static factory from ErrorCode
│       ├── LoginResponse.java
│       ├── Response.java
│       └── UserResponse.java
├── handlers/
│   ├── commands/
│   │   ├── EncodePasswordHandler.java
│   │   ├── LoginUserHandler.java
│   │   ├── RegisterUserHandler.java       # Result.Success(UUID id) instead of UserEntity
│   │   └── impl/ (…HandlerImpl.java)
│   └── queries/
│       ├── GetCurrentUserQuery.java       # Result.Success(UUID id, String name, String email)
│       └── impl/GetCurrentUserQueryImpl.java
├── persistence/
│   ├── model/UserEntity.java              # was jpa/entities; @GeneratedValue(strategy = UUID)
│   └── repositories/UserRepository.java   # was jpa/repositories; no @Repository
├── exception/
│   ├── BusinessException.java             # replaces models/BaseException (ctor + Builder)
│   └── ErrorCode.java                     # was models/ErrorCode; + INVALID_CREDENTIALS
├── security/                              # + AuthenticatedUser (was models/)
└── session/                               # unchanged

src/test/java/dev/leocamacho/demo/tests/
├── handlers/commands/RegisterUserHandlerTests.java   # updated
├── handlers/commands/LoginUserHandlerTests.java      # new
├── handlers/queries/GetCurrentUserQueryTests.java    # new
├── structure/StructureValidationTests.java           # rules rewritten
└── api/…                                             # imports only + invalid-credentials step
src/test/resources/features/Authentication.feature    # + invalid-credentials scenario
```

**Structure Decision**: single project; layout per `docs/ARCHITECTURE.md` §2. `GlobalExceptionHandler` sits at the
root of `api` (§7 places it "in `api`"; it is not a controller, so it does not belong in `api.controllers` where the
controller ArchUnit rule applies).

## Design

### Handlers (§3)

- `RegisterUserHandler.Result.Success(UUID id)`; impl maps the saved entity to it.
- `GetCurrentUserQuery.Result.Success(UUID id, String name, String email)`; `UserNotFound` becomes a record.
- `LoginUserHandlerImpl` catches `org.springframework.security.core.AuthenticationException` from
  `AuthenticationManager.authenticate` and returns `Result.InvalidCredentials()` (today unreachable).
- `EncodePasswordHandler` unchanged apart from package imports.

### Controllers (§4)

Methods return `ResponseEntity<?>`; each `switch` maps success to `ResponseEntity.ok(<api.responses DTO>)` and
expected outcomes to `ResponseEntity.badRequest().body(ErrorResponse.of(code, correlationId, params))`, keeping
status 400 and the existing codes/messages:

| Endpoint | Result | Status | Body |
|---|---|---|---|
| `POST /api/public/register` | `Success` | 200 | `Response{result=id}` |
| | `InvalidFields` | 400 | `REQUIRED_FIELDS` (1002) + missing fields in `params` |
| | `EmailAlreadyExists` | 400 | `EMAIL_ALREADY_EXISTS` (2002) |
| `POST /api/public/login` | `Success` | 200 | `LoginResponse` |
| | `InvalidCredentials` | 400 | **`INVALID_CREDENTIALS` (2003)** — new; was `UNKNOWN_ERROR` (1003) |
| `GET /api/private/users/current` | `Success` | 200 | `UserResponse` |
| | `UserNotFound` | 400 | `INVALID_USER` (2001) |

### Errors (§7)

- `exception.ErrorCode`: existing entries + `INVALID_CREDENTIALS(2003, "Invalid credentials")`. Accessors
  `code()`/`message()` kept (used by security and tests).
- `exception.BusinessException extends RuntimeException`: `(ErrorCode, Object... params)` constructor, private
  Builder ctor, `builder(ErrorCode).param(..).build()`, `{i}` placeholder formatting, getters.
- `api.GlobalExceptionHandler` (`@RestControllerAdvice`): `BusinessException` → 500 `ErrorResponse`
  (reference §7); `Throwable` fallback → 400 `UNKNOWN_ERROR`, preserving today's behavior for unexpected errors.

### Persistence (§6)

- `UserEntity`: `@Id @GeneratedValue(strategy = GenerationType.UUID)`; builder and constructor stop assigning
  `UUID.randomUUID()`. No schema change (column already `UUID`), so no new changeset.
- `UserRepository`: remove `@Repository`.

### ArchUnit (`StructureValidationTests`)

- Controllers: `..api.controllers..`; return types in `api.responses` or `http.ResponseEntity`; params in
  `api.request`; other checks unchanged.
- Command handler impls: params only from `commands` (drop `jpa.entities`).
- **New**: classes in `..handlers.commands` / `..handlers.queries` (contracts, not `impl`) must not depend on
  `..persistence..`.
- Repositories: `*Repository` in `..persistence.repositories..` must be interfaces assignable to `JpaRepository`
  and must **not** be annotated with `@Repository`.
- Layers use fully qualified roots (`dev.leocamacho.demo.api..`, `…handlers..`, `…persistence..`,
  `…security..`) so test packages such as `tests.api` are not captured by `..api..`.

### Docs

`CLAUDE.md` Architecture/Package conventions/Handler pattern rewritten for the new layout; the "Target
architecture" paragraph trimmed to a pointer to `docs/ARCHITECTURE.md`.

## Risks

- **Package moves break imports in tests and Cucumber glue** → compile-driven; build catches it.
- **`@GeneratedValue` with a pre-set id** would make Spring Data `merge` instead of `persist` → builder must not set
  the id (unit test stubs set it explicitly).
- **ArchUnit pattern overlap** (`..api..` matching `tests.api`) → fully qualified layer roots.
- **Login contract change** (1003 → 2003 for bad credentials) → called out in the PR; status stays 400.
- E2E scenarios are not executed by the workflow (need a running app) → human runs `./gradlew e2eTest`.

## Complexity Tracking

No constitution violations.
