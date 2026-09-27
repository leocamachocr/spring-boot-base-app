# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and run all tests (includes Checkstyle)
./gradlew build

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "dev.leocamacho.demo.tests.handlers.commands.RegisterUserHandlerTests"

# Run Checkstyle only
./gradlew checkstyleMain checkstyleTest

# Boot the app locally (H2 file DB at C:/data/testdb123)
./gradlew bootRun
```

Always run `./gradlew build` before declaring a task done — it runs both tests and Checkstyle.

Gradle 8.14 cannot run on JDK 25+; if `JAVA_HOME` points to a newer JDK, run Gradle with a JDK 21 (e.g. `JAVA_HOME=~/.jdks/openjdk-21.0.2`).

## Architecture

Spring Boot 4 / Java 21 app following **CQRS** with a strict layered architecture enforced at test time by **ArchUnit** (`StructureValidationTests`).

### Layer rules (enforced)
```
api (rests, types, exceptions)
  └── handlers (commands, queries)
        └── repositories (jpa)
```
- `api` may only be accessed by `security` and tests.
- `handlers` may only be accessed by `api` and tests.
- `repositories` may only be accessed by `handlers`, `security`, and tests.

### Package conventions (also enforced by ArchUnit)
| Package | Pattern | Constraints |
|---|---|---|
| `api.rests` | `*Controller` | `@RestController` + `@RequestMapping`; ≤7 public methods; params/return types only from `api.types` |
| `handlers.commands.impl` | `*HandlerImpl` | `@Component`/`@Service`; exactly 1 public method (`handle`); implements its `*Handler` interface; params/return types only from `jpa.entities` or `commands` |
| `jpa.repositories` | `*Repository` | Must have `@Repository` |

### Handler pattern
Each use case has:
- An **interface** declaring a `Result` sealed interface plus the entry method:
  - Commands (`handlers/commands/`): a `Command` record and `Result handle(Command)`; impl is `*HandlerImpl` in `commands/impl/`.
  - Queries (`handlers/queries/`): `Result query(...)` with plain params; impl is `*QueryImpl` in `queries/impl/`. The ArchUnit handler rule only covers `commands.impl`, so queries are convention-only.
- The impl annotated `@Component`. Results typically carry `jpa.entities` (e.g. `UserEntity`); controllers map them to `api.types`.

Controllers pattern-match exhaustively on the `Result` sealed type and throw `BaseException` (built with `BaseException.exceptionBuilder()` + an `ErrorCode`) for error cases; `ExceptionManagerController` converts those to `ErrorResponse`. `models` (`BaseException`, `ErrorCode`, `AuthenticatedUser`) and `session` are shared and sit outside the enforced layers.

### Security
- Routes under `/api/private/**` require a JWT; routes under `/api/public/**` are open.
- `JwtRequestFilter` runs on every request and stores a `Session` (the Spring `Authentication`) — built from JWT claims, or anonymous if no valid token. Read the current user via `SessionContextHolder.getSession()`.
- JWT secret is configured in `application.yaml` under `jwt.secret`.

### Database
H2 file-based (`jdbc:h2:file:C:/data/testdb123`); migrations managed by **Liquibase** (`db.changelog-master.xml`). The H2 console is available at `/h2`.

## Testing

Two test suites:

1. **Unit/handler tests** — plain JUnit 5, test `*HandlerImpl` in isolation (e.g., `RegisterUserHandlerTests`).
2. **Cucumber E2E tests** — runner at `CucumberTestRunner`; feature files live in `src/test/resources/features/`. Steps use `ScenarioContext` (PicoContainer-injected) to share state between steps. `ApiContext` holds the bearer token between steps. `UserApi` wraps REST-Assured calls; request records are built from Gherkin data tables via `CucumberAdapters.mapToInstance`.
   - These are **black-box tests against a running server** (no `@SpringBootTest`): start the app with `./gradlew bootRun` first. Target defaults to `http://localhost:8080`, overridable with `-Dhost=` / `-Dport=` system properties (see `ApiVerbs`).
   - The runner uses the JUnit 4 `@RunWith(Cucumber.class)` API while Gradle is configured with `useJUnitPlatform()` and no vintage engine, so `./gradlew test` does not pick it up — run it from the IDE.

Feature files support a `{String:N}` placeholder (generates a random N-char string) and `{String:N:$var}` to save the value in a named variable for reuse as `{$var}`.

## Checkstyle

Sun coding conventions enforced at severity `error` (config in `config/checkstyle/checkstyle.xml`). `checkstyleMain` runs before `checkstyleTest`, so a failure in main hides test violations — use `--continue` to see both. Key rules to watch:
- Max line length is **120** (raised from the default 80).
- No star imports (including `import static ...*`), no unused imports, files must end with a newline, tabs forbidden (use spaces).
- `static final` fields must be `UPPER_SNAKE_CASE` — this includes loggers and ArchUnit `@ArchTest` rules. Method names are camelCase with no underscores, test methods included.
- `NeedBraces` — `if`/`else` always need braces, even on one-liners.
- `MagicNumber` rule — extract numeric literals to named constants.
- `TodoComment` rule — no `TODO` comments may remain in submitted code.
