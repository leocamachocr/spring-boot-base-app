# Spring Boot Base App Constitution

## Core Principles

### I. CQRS Use Cases (NON-NEGOTIABLE)
Every use case is a command or a query, never both.
- Commands: interface in `handlers/commands/` with a `Command` record, a `Result` sealed interface and
  `Result handle(Command)`; implementation `*HandlerImpl` in `handlers/commands/impl/`, annotated `@Component`,
  with exactly one public method.
- Queries: interface in `handlers/queries/` with `Result query(...)`; implementation `*QueryImpl` in
  `handlers/queries/impl/`.
- Errors are modeled as `Result` variants, not exceptions. Controllers pattern-match exhaustively on `Result`
  and throw `BaseException` (with an `ErrorCode`) only at the API edge.

### II. Enforced Layering
`api` → `handlers` → `repositories`, verified by ArchUnit in `StructureValidationTests`.
- `api` is accessed only by `security` and tests; `handlers` only by `api` and tests; `repositories` only by
  `handlers`, `security` and tests.
- Controllers (`api.rests`, `*Controller`) take and return only `api.types`; ≤7 public methods, each with a
  mapping annotation.
- Architecture rules are changed only deliberately and in the same PR that needs the change, with the reason
  stated in the PR description. Never weaken a rule to make a feature pass.

### III. Tests Accompany Every Change
- New or changed handlers get JUnit 5 unit tests in isolation (Mockito for repositories/collaborators).
- New or changed endpoints get a Cucumber scenario under `src/test/resources/features/`, run via
  `./gradlew e2eTest` against a running app (not part of `build`).
- Bug fixes include a test that fails without the fix.

### IV. Green Build Is the Definition of Done
`./gradlew build` (unit + ArchUnit tests + Checkstyle) MUST pass before a task is considered complete.
Checkstyle violations are fixed, never suppressed. No `TODO` comments in delivered code: implement it or record
it as an open decision in the spec.

### V. Simplicity
Prefer the simplest solution that satisfies the spec. No speculative abstractions, no new dependencies or
modules without a requirement in the spec that needs them. Follow existing patterns in the codebase before
introducing new ones.

## Technical Constraints

- Java 21, Spring Boot 4, Gradle (run Gradle with JDK 21; Gradle 8.14 does not run on JDK 25+).
- Persistence: JPA over H2; every schema change is a new Liquibase changeset registered in
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

This constitution takes precedence over other practices for spec-driven work; `CLAUDE.md` holds the detailed
runtime guidance (commands, architecture, testing). Plans MUST include a constitution check and justify any
deviation. Amendments are made through a PR that updates this file, bumps the version (MAJOR: principle removed
or redefined; MINOR: principle or section added; PATCH: clarifications) and updates dependent templates.

**Version**: 1.0.0 | **Ratified**: 2026-09-27 | **Last Amended**: 2026-09-27
