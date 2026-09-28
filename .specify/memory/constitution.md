# Spring Boot Base App Constitution

## Core Principles

### I. Reference Architecture (NON-NEGOTIABLE)
`docs/ARCHITECTURE.md` is the target architecture. Every spec, plan and implementation follows it; plans cite the
sections they rely on and justify any deviation in the Complexity Tracking table.
- Top-level packages: `api` (`controllers`, `request`, `responses`), `handlers` (`commands`, `queries`),
  `providers`, `persistence` (`model`, `repositories`) and `exception`. `security` and `session` are cross-cutting
  and stay outside these layers. No generic `business` or `service` package.
- JPA entities never leave `persistence`/`handlers`; request/response DTOs never leave `api`. Mapping between
  them is explicit.
- Every external integration (mail, SMS, payments, third-party APIs) sits behind a `Provider` interface in
  `providers`; handlers depend only on the interface.
- Entities use UUID identifiers.

### II. CQRS Use Cases
Every use case is a command or a query, never both.
- One interface per use case, with its `Command`/`Query` record and its `Result` sealed interface nested in the
  same file. Its implementation (`*Impl`, `@Component`, exactly one public method) is the business component.
- `Result` variants carry their own records, never JPA entities.
- Controllers contain no business logic: they build the `Command`/`Query`, call the handler through its interface
  and translate the `Result` to HTTP with an exhaustive `switch`.

### III. Errors: Results First, Exceptions for Rollback
- Expected business outcomes (conflict, not found, invalid credentials...) are `Result` variants, never exceptions
  or `null`.
- `BusinessException` (unchecked, built on an `ErrorCode` with a constructor and a Builder) is thrown only to roll
  back an operation already in progress. Subtypes only when they add value.
- A single `@RestControllerAdvice` handles the base exception.

### IV. Enforced Layering
`api` → `handlers` → `persistence`, verified by ArchUnit in `StructureValidationTests`.
- `api` is accessed only by `security` and tests; `handlers` only by `api` and tests; `persistence` only by
  `handlers`, `security` and tests; `providers` implementations are reached only through their interfaces.
- Architecture rules are changed only deliberately and in the same PR that needs the change, with the reason
  stated in the PR description. Never weaken a rule to make a feature pass.

### V. Tests Accompany Every Change
- New or changed handlers get JUnit 5 unit tests in isolation (Mockito for repositories, providers and other
  collaborators).
- New or changed endpoints get a Cucumber scenario under `src/test/resources/features/`, run via
  `./gradlew e2eTest` against a running app (not part of `build`).
- Bug fixes include a test that fails without the fix.

### VI. Green Build Is the Definition of Done
`./gradlew build` (unit + ArchUnit tests + Checkstyle) MUST pass before a task is considered complete.
Checkstyle violations are fixed, never suppressed. No `TODO` comments in delivered code: implement it or record
it as an open decision in the spec.

### VII. Simplicity
Prefer the simplest solution that satisfies the spec. No speculative abstractions, no new dependencies or
modules without a requirement in the spec that needs them. Follow the reference architecture and existing
patterns before introducing new ones.

## Technical Constraints

- Java 26, Spring Boot 4.1 (Spring Security 7.1), Gradle 9.8.
- Persistence: Spring Data JPA over H2; every schema change is a new Liquibase changeset registered in
  `db.changelog-master.xml`. Existing changesets are never edited.
- Security: `/api/public/**` is open, `/api/private/**` requires a JWT. The current user is read via
  `SessionContextHolder.getSession()`. Authentication or authorization changes must be called out explicitly in
  the spec and PR.
- Code style: Sun conventions via Checkstyle, max line length 120, constants in `UPPER_SNAKE_CASE`, no magic
  numbers, no star imports.

## Development Workflow

- Work arrives as GitHub issues and is processed with the `/speckit-issue` skill: spec and plan are published
  on the issue and require human approval (label `speckit:approved`) before any implementation.
- One issue = one feature branch = one PR that references the issue (`Closes #N`). Never commit to `main`.
- Commit messages follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `build:`).
- Ambiguity is resolved by asking on the issue, not by guessing. Unresolved `[NEEDS CLARIFICATION]` markers
  block planning.

## Governance

This constitution takes precedence over other practices for spec-driven work; `docs/ARCHITECTURE.md` details the
architecture it mandates and `CLAUDE.md` holds the detailed runtime guidance (commands, testing). Plans MUST
include a constitution check and justify any deviation. Amendments are made through a PR that updates this file,
bumps the version (MAJOR: principle removed or redefined; MINOR: principle or section added; PATCH:
clarifications) and updates dependent templates. Changes to the architecture reference follow the same process.

**Version**: 2.0.2 | **Ratified**: 2026-09-27 | **Last Amended**: 2026-09-28
