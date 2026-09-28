# Tasks: Migrate Codebase to the Reference Architecture

**Input**: Design documents from `specs/005-reference-architecture-migration/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/http-api.md

**Tests**: Required by the constitution (principle V): handler unit tests, ArchUnit rules, a Cucumber scenario for
the changed login contract.

Paths are relative to `src/main/java/dev/leocamacho/demo/` (main) and `src/test/java/dev/leocamacho/demo/tests/`
(test) unless written in full.

## Phase 1: Setup

- [X] T001 Verify `.gitignore` covers Java/Gradle outputs (`build/`, `.gradle/`) at repository root

## Phase 2: Foundational (package moves — blocks every story)

- [X] T002 Move `jpa/entities/UserEntity.java` to `persistence/model/UserEntity.java` and `jpa/repositories/UserRepository.java` to `persistence/repositories/UserRepository.java`, updating packages and imports
- [X] T003 Move `models/ErrorCode.java` to `exception/ErrorCode.java` and add `INVALID_CREDENTIALS(2003, "Invalid credentials")`
- [X] T004 Move `models/AuthenticatedUser.java` to `security/AuthenticatedUser.java`, updating `security/JwtProvider.java`, `security/UserAuthenticationQuery.java` and `handlers/commands/impl/LoginUserHandlerImpl.java`
- [X] T005 Split `api/types/` into `api/request/` (`LoginUserRequest`, `RegisterUserRequest`) and `api/responses/` (`ErrorResponse`, `LoginResponse`, `Response`, `UserResponse`); move `api/rests/*Controller.java` to `api/controllers/`
- [X] T006 Update every import in `src/test/java` (Cucumber glue, fakers, handler tests) for the moved packages

## Phase 3: User Story 2 - Codebase follows the reference (P1)

**Goal**: handler contracts free of entities, Results for expected outcomes, single exception model.
**Independent Test**: `./gradlew test` passes; no legacy package remains (`quickstart.md` grep).

- [X] T007 [P] [US2] Create `exception/BusinessException.java` (unchecked; `(ErrorCode, Object... params)` ctor, private Builder ctor, `builder(ErrorCode).param(..).build()`, `{i}` formatting) and delete `models/BaseException.java`
- [X] T008 [P] [US2] Change `handlers/commands/RegisterUserHandler.java` `Result.Success` to `Success(UUID id)` and map the saved entity in `handlers/commands/impl/RegisterUserHandlerImpl.java`
- [X] T009 [P] [US2] Change `handlers/queries/GetCurrentUserQuery.java` to `Success(UUID id, String name, String email)` and `record UserNotFound()`; map in `handlers/queries/impl/GetCurrentUserQueryImpl.java`
- [X] T010 [P] [US2] Catch `AuthenticationException` in `handlers/commands/impl/LoginUserHandlerImpl.java` and return `Result.InvalidCredentials()`
- [X] T011 [US2] `persistence/model/UserEntity.java`: `@GeneratedValue(strategy = GenerationType.UUID)`, stop assigning ids in constructor/builder; remove `@Repository` from `persistence/repositories/UserRepository.java`
- [X] T012 [P] [US2] Update `handlers/commands/RegisterUserHandlerTests.java` for `Success(UUID id)` (stubbed entity gets an explicit id)
- [X] T013 [P] [US2] Add `handlers/commands/LoginUserHandlerTests.java` (success; `BadCredentialsException` → `InvalidCredentials`)
- [X] T014 [P] [US2] Add `handlers/queries/GetCurrentUserQueryTests.java` (found → mapped fields; unknown email and null email → `UserNotFound`)

## Phase 4: User Story 1 - No regression for API clients (P1)

**Goal**: identical HTTP contract (see `contracts/http-api.md`), except invalid credentials → 2003.
**Independent Test**: `./gradlew bootRun` + `./gradlew e2eTest`.

- [X] T015 [US1] Add static factory `ErrorResponse.of(ErrorCode, UUID correlationId, String... params)` in `api/responses/ErrorResponse.java`
- [X] T016 [P] [US1] `api/controllers/RegisterUserController.java`: return `ResponseEntity<?>`; `Success` → 200 `Response(id)`, `InvalidFields` → 400 `REQUIRED_FIELDS` + fields, `EmailAlreadyExists` → 400 `EMAIL_ALREADY_EXISTS`
- [X] T017 [P] [US1] `api/controllers/LoginUserController.java`: return `ResponseEntity<?>`; `InvalidCredentials` → 400 `INVALID_CREDENTIALS`
- [X] T018 [P] [US1] `api/controllers/UserQueriesController.java`: return `ResponseEntity<?>`; `UserNotFound` → 400 `INVALID_USER`
- [X] T019 [US1] Replace `api/exceptions/ExceptionManagerController.java` with `api/GlobalExceptionHandler.java` (`@RestControllerAdvice`; `BusinessException` → 500, `Throwable` → 400 `UNKNOWN_ERROR`)
- [X] T020 [US1] Add scenario "User login with invalid credentials" in `src/test/resources/features/Authentication.feature` with steps in `api/steps/AuthenticationSteps.java`

## Phase 5: User Story 3 - Violations caught automatically (P2)

**Goal**: ArchUnit encodes the new layout.
**Independent Test**: `./gradlew test --tests "*StructureValidationTests"`.

- [X] T021 [US3] Rewrite rules in `structure/StructureValidationTests.java`: controllers in `..api.controllers..` (returns `api.responses`/`http.ResponseEntity`, params `api.request`); handler impl params only `commands`; new rule "handler contracts must not depend on `..persistence..`"; repositories are `JpaRepository` interfaces without `@Repository`; layers with fully qualified roots

## Phase 6: Polish

- [X] T022 Rewrite Architecture / Package conventions / Handler pattern / Security sections of `CLAUDE.md` for the new layout
- [X] T023 Run `./gradlew build` (JDK 21) and the legacy-package grep from `quickstart.md`

## Dependencies & Execution Order

- Phase 2 blocks everything (compilation).
- US2 (Phase 3) before US1 (Phase 4): controllers consume the new Result shapes.
- US3 (Phase 5) after US1/US2: rules validate the final layout.
- Polish last.

## Parallel Opportunities

- T007–T010 touch different files; T012–T014 are independent test files; T016–T018 are independent controllers.

## Implementation Strategy

Single PR (the constitution blocks other issues until this lands): Foundational → US2 → US1 → US3 → Polish, with
`./gradlew build` as the gate.
