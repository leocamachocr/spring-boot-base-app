# Feature Specification: Migrate Codebase to the Reference Architecture

**Feature Branch**: `speckit/005-reference-architecture-migration`
**Created**: 2026-09-27
**Status**: Draft
**Input**: GitHub issue #5 — "Migrar el código a la arquitectura de referencia (docs/ARCHITECTURE.md)"

## Context

The project adopted `docs/ARCHITECTURE.md` as its mandatory reference architecture (constitution v2.0.0,
principle I). The existing code predates it. This feature brings the existing code in line with the reference
so that every later feature is built on a consistent base. It is a structural migration: the product behavior
seen by API clients stays the same, except where noted below.

The "users" of this feature are the maintainers of the codebase (humans and the spec-driven agent), plus the
API clients whose experience must not regress.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - API clients see no regression (Priority: P1)

An API client registers a user, logs in, and reads the current user exactly as before the migration: same
routes, same request and response bodies, same HTTP status codes for success and for expected business
outcomes (missing fields, duplicated email, unknown user).

**Why this priority**: A structural migration that breaks clients has negative value. Everything else is
secondary to keeping the public contract intact.

**Independent Test**: Run the existing end-to-end scenarios (register with valid data, duplicated email) against
the migrated app and compare responses with the pre-migration contract.

**Acceptance Scenarios**:

1. **Given** a new email, **When** the client registers, **Then** it receives a success response containing the
   new user's identifier.
2. **Given** an already registered email, **When** the client registers again, **Then** it receives the same
   error status, error code and message as before the migration.
3. **Given** a registration with missing name, email or password, **When** the client registers, **Then** it
   receives the "required fields" error listing the missing fields, as before.
4. **Given** valid credentials, **When** the client logs in and requests the current user with the returned
   token, **Then** it receives the user's name and email.
5. **Given** a request to a private route without a valid token, **When** it is sent, **Then** the client
   receives the same "unauthorized" response as before.

---

### User Story 2 - Maintainers work on a codebase that follows the reference (Priority: P1)

A maintainer (or the spec-driven agent) opens the project and finds the package layout, handler contracts and
error handling described in `docs/ARCHITECTURE.md`, with no legacy layout left to reconcile.

**Why this priority**: The constitution blocks all other issues until this migration lands; the value of the
reference architecture depends on the code actually following it.

**Independent Test**: Inspect the source tree and run the automated architecture checks; they pass and no legacy
package remains.

**Acceptance Scenarios**:

1. **Given** the migrated code, **When** the source tree is inspected, **Then** only the reference packages exist
   (`api` with `controllers`/`request`/`responses`, `handlers`, `persistence`, `exception`, plus the cross-cutting
   `security` and `session`), and none of the legacy packages remain.
2. **Given** any use case, **When** its handler contract is read, **Then** its results expose only their own data
   records, never persistence entities.
3. **Given** an expected business outcome (including invalid login credentials), **When** it happens, **Then** it
   is modeled as a typed result and translated to HTTP by the controller, not raised as an exception.

---

### User Story 3 - Architecture violations are caught automatically (Priority: P2)

When a future change breaks the reference architecture (e.g., a handler result exposing an entity, an entity
reaching the API layer, a controller in the wrong package), the build fails with an explicit message.

**Why this priority**: Keeps the migration from eroding; the constitution requires enforcement by tests.

**Independent Test**: Run the build; the architecture tests encode the new rules and pass on the migrated code.

**Acceptance Scenarios**:

1. **Given** the migrated code, **When** the build runs, **Then** the architecture checks pass.
2. **Given** a handler result type that references a persistence entity, **When** the build runs, **Then** the
   architecture checks fail.

### Edge Cases

- Invalid login credentials: today the underlying authentication failure escapes as an uncontrolled error (and
  the code path meant to handle it is unreachable). After the migration it MUST be an explicit expected outcome
  with its own error code and the same HTTP status family as other expected outcomes.
- Existing users stored before the migration MUST remain usable (login and current-user lookup), since
  identifiers are already UUIDs.
- Unexpected (non-business) failures MUST still produce a structured error response with a correlation id.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The public API routes, request bodies and success response bodies MUST remain unchanged.
- **FR-002**: Expected business outcomes (missing fields, duplicated email, user not found, invalid credentials)
  MUST be returned with the existing error status and error body shape (message, code, correlation id,
  params); existing error codes and messages MUST be preserved.
- **FR-003**: Invalid login credentials MUST be reported as an expected outcome with a dedicated error code
  instead of the generic "unknown error".
- **FR-004**: Code MUST be organized in the reference packages: `api.controllers`, `api.request`, `api.responses`,
  `handlers.commands`, `handlers.queries`, `persistence.model`, `persistence.repositories`, `exception`; with
  `security` and `session` as cross-cutting packages. Legacy packages (`api.rests`, `api.types`,
  `api.exceptions`, `jpa.*`, `models`) MUST NOT remain.
- **FR-005**: Every handler result MUST carry its own data records; no persistence entity may appear in a handler
  contract or leave the handler implementation.
- **FR-006**: A single unchecked business exception with an error code, a constructor and a builder MUST exist,
  reserved for rollback scenarios; a single centralized error handler MUST handle it and any unexpected error.
- **FR-007**: Entities MUST use UUID identifiers generated by the persistence layer; existing stored data MUST
  remain valid without editing existing migrations.
- **FR-008**: Repositories MUST be plain Spring Data interfaces without the manual repository annotation.
- **FR-009**: The automated architecture checks MUST be updated (not removed) to enforce the new layout, handler
  contracts free of entities, and repository conventions.
- **FR-010**: A `providers` package MUST NOT be created, since no current use case integrates with an external
  system.
- **FR-011**: The project guide (`CLAUDE.md`) MUST describe the new structure instead of the legacy one.

### Key Entities

- **User**: a registered person with identifier (UUID), name, email and password hash. Unchanged in content;
  only its location in the codebase and how its identifier is generated change.
- **Error code**: the catalog of business error codes and messages returned to clients. Gains one entry for
  invalid credentials.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the existing end-to-end scenarios pass against the migrated app without changing their
  Gherkin steps.
- **SC-002**: 0 references to legacy packages remain in the source code.
- **SC-003**: 0 handler contracts expose persistence entities (verified by an automated check).
- **SC-004**: The full build (unit tests, architecture tests, style checks) passes.
- **SC-005**: An invalid login attempt returns a dedicated, documented error code in 100% of cases instead of the
  generic unknown error.

## Assumptions

- "No change in behavior" is measured at the HTTP contract: routes, bodies, status codes and existing error
  codes/messages. Internal names may change.
- Handler interface names (`RegisterUserHandler`, `LoginUserHandler`, `EncodePasswordHandler`,
  `GetCurrentUserQuery`) are kept; the reference's `*CommandHandler` naming is illustrative, and renaming would add
  churn without value.
- Implementations stay in `commands.impl` / `queries.impl` sub-packages, which the reference allows (they live under
  `handlers.commands` / `handlers.queries`).
- The existing error status for expected outcomes (400) is kept to preserve the contract, even though other codes
  (e.g., 409 for a duplicated email) would be more precise; changing them is a separate decision.
- The user table already stores UUID identifiers, so no new database migration is needed for FR-007.
- Request validation annotations (Bean Validation) are out of scope: validation is done by the handlers today and
  adding the validation starter is a dependency change not required by the issue.
- The authenticated-user model used by security moves to the `security` package, since `models` disappears.
- New features, new endpoints and changes to authentication/authorization rules are out of scope.
