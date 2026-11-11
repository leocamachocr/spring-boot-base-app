# Tasks: Conversational Chatbot (Stage 1: Plain Chat)

**Input**: Design documents from `specs/011-gemini-chatbot/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/chat-api.md, quickstart.md

**Tests**: Required by the constitution (principle V): handler unit tests, provider tests with
`MockRestServiceServer`, an ArchUnit rule, and Cucumber scenarios for the new endpoint. No test may call Google.

Paths are relative to `src/main/java/dev/leocamacho/demo/` (main) and `src/test/java/dev/leocamacho/demo/tests/`
(test) unless written in full. Every bean that belongs to the chatbot (controller, handler impl, Gemini provider)
carries `@ConditionalOnProperty(name = "features.chatbot.enabled", havingValue = "true")`.

## Phase 1: Setup

- [X] T001 Add configuration to `src/main/resources/application.yaml`: `features.chatbot.enabled: false`, `google.ai.api-key: ${GOOGLE_AI_API_KEY:}`, `google.ai.model: gemini-flash-latest`, `google.ai.base-url: https://generativelanguage.googleapis.com/v1beta`, `google.ai.timeout: 30s`, `chatbot.history-max-turns: 20` (contracts/chat-api.md, "Configuration contract")
- [X] T002 [P] Add `RESOURCE_NOT_FOUND(1004, "Resource not found")`, `CHAT_INVALID_MESSAGE(3001, "Invalid chat message")`, `CHAT_CONVERSATION_NOT_FOUND(3002, "Conversation not found")`, `CHAT_ASSISTANT_UNAVAILABLE(3003, "The assistant is unavailable")`, `CHAT_NO_ANSWER(3004, "The assistant could not answer")` to `exception/ErrorCode.java`, with a `// Chat 3000 - 3999` section comment

## Phase 2: Foundational (blocks every story)

- [X] T003 Create `src/main/resources/db/changelog/004-create-chat-tables.sql` with `chat_conversations` (`id UUID PK`, `owner_id UUID NOT NULL FK users(id)`, `created_at TIMESTAMP NOT NULL`, `last_activity_at TIMESTAMP NOT NULL`, index on `owner_id`) and `chat_messages` (`id UUID PK`, `conversation_id UUID NOT NULL FK chat_conversations(id)`, `role VARCHAR(16) NOT NULL`, `kind VARCHAR(32) NOT NULL`, `content CLOB NOT NULL`, `created_at TIMESTAMP(9) NOT NULL`, index on `(conversation_id, created_at)`), and register it as `<changeSet id="3" author="lcamacho">` in `src/main/resources/db/changelog/db.changelog-master.xml`; do not touch existing changesets or `003-rename-tables.sql`
- [X] T004 [P] Create `persistence/model/ConversationEntity.java` (`@Table(name = "chat_conversations")`, UUID id with `GenerationType.UUID`, `ownerId`, `createdAt`, `lastActivityAt` as `Instant`, `@OneToMany(mappedBy = "conversation", cascade = ALL)` list of messages with an `addMessage(ChatMessageEntity)` helper that sets the back-reference)
- [X] T005 [P] Create `persistence/model/ChatMessageEntity.java` (`@Table(name = "chat_messages")`, UUID id, `@ManyToOne(fetch = LAZY)` conversation, `@Enumerated(STRING)` `role` and `kind`, `@Lob content`, `createdAt` as `Instant`) plus `persistence/model/ChatRole.java` (`USER`, `ASSISTANT`) and `persistence/model/ChatMessageKind.java` (`TEXT` only)
- [X] T006 [P] Create `persistence/repositories/ConversationRepository.java` (`extends JpaRepository<ConversationEntity, UUID>`, `Optional<ConversationEntity> findByIdAndOwnerId(UUID id, UUID ownerId)`; no `@Repository`)
- [X] T007 [P] Create `persistence/repositories/ChatMessageRepository.java` (`extends JpaRepository<ChatMessageEntity, UUID>`, `List<ChatMessageEntity> findByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId, Limit limit)`; no `@Repository`)
- [X] T008 [P] Create `providers/ChatProvider.java`: `Response reply(Request request)`; nested `record Request(List<Turn> history, List<ToolDefinition> tools)`, `record Turn(Role role, String text)`, `enum Role { USER, ASSISTANT }`, `record ToolDefinition(String name, String description, Map<String, Object> parameters)`, `sealed interface Response` with `Answer(String text)`, `Unavailable(Reason reason)`, `NoAnswer(NoAnswerReason reason)`, `enum Reason { AUTH, QUOTA, TIMEOUT, SERVICE_ERROR }`, `enum NoAnswerReason { EMPTY, BLOCKED }` (data-model.md, "Provider contract")
- [X] T009 [P] Create `handlers/tools/ChatTool.java` interface: `String name()`, `String description()`, `Map<String, Object> parameters()`, `Map<String, Object> execute(UUID userId, Map<String, Object> arguments)`; no implementations (research R7)
- [X] T010 [P] Create `handlers/commands/SendChatMessageHandler.java`: `Result handle(Command command)`; `record Command(UUID userId, UUID conversationId, String message)`; `sealed interface Result` with `Answered(UUID conversationId, String answer)`, `InvalidMessage(String reason)`, `ConversationNotFound()`, `AssistantUnavailable()`, `NoAnswer()` (no persistence imports)
- [X] T011 [P] Add an ArchUnit rule `PROVIDER_IMPLEMENTATIONS_ARE_HIDDEN` to `structure/StructureValidationTests.java`: no classes outside `dev.leocamacho.demo.providers..` depend on classes in `dev.leocamacho.demo.providers.gemini..` (research R7; reason goes in the PR description)

**Checkpoint**: `./gradlew build` passes (contracts compile, schema migrates, ArchUnit green).

## Phase 3: User Story 1 - Send a message and get an answer (P1) 🎯 MVP

**Goal**: a signed-in user sends a message without a conversation id and gets the assistant's answer and a new
conversation id.
**Independent Test**: with the flag on and a real key, `POST /api/private/chat/messages {"message":"Hello"}` returns
200 with a non-empty `answer` and a `conversationId`; without a JWT it returns 401.

### Tests for User Story 1

- [ ] T012 [P] [US1] Create `providers/GeminiChatProviderTests.java` with `MockRestServiceServer` bound to a `RestClient.Builder`: request goes to `{base-url}/models/{model}:generateContent`, carries the key only in the `x-goog-api-key` header (never in the URL), maps `USER` → `"user"` and `ASSISTANT` → `"model"`, omits `tools` when the list is empty, sends `functionDeclarations` when not empty; a candidate with text parts → `Answer` with the parts concatenated
- [ ] T013 [P] [US1] Create `handlers/commands/SendChatMessageHandlerTests.java` (Mockito, strict stubs, mocks for `ConversationRepository`, `ChatMessageRepository`, `ChatProvider`, `ObjectProvider<ChatTool>`): new conversation + `Answer` → `Answered` with the saved conversation id, one `save` holding the user turn then the assistant turn (`kind = TEXT`), provider request has empty history plus the user turn and an empty `tools` list

### Implementation for User Story 1

- [ ] T014 [P] [US1] Create `providers/gemini/GeminiDtos.java` with package-private records for the request (`contents[{role, parts[{text}]}]`, `tools[{functionDeclarations[{name, description, parameters}]}]`) and the response (`candidates[{content{parts[{text}]}, finishReason}]`, `promptFeedback{blockReason}`), `@JsonInclude(NON_NULL/NON_EMPTY)` so empty `tools` is omitted
- [ ] T015 [US1] Create `providers/gemini/GeminiChatProvider.java` (`@Component`, conditional on the flag, implements `ChatProvider`): `RestClient` built from `RestClient.Builder` with `google.ai.base-url`, default header `x-goog-api-key`, and request timeouts from `google.ai.timeout`; maps `Request` → DTOs and a text candidate → `Answer`; logs model and HTTP status only, never the key or bodies (depends on T014)
- [ ] T016 [US1] Create `handlers/commands/impl/SendChatMessageHandlerImpl.java` (`@Component`, conditional on the flag, one public method `handle`): reject a null message as `InvalidMessage` (T031 completes validation), build history (empty for a new conversation) + user turn stamped with `Instant.now()`, map `ObjectProvider<ChatTool>` beans to `ToolDefinition`s, call `chatProvider.reply(...)` with **no** `@Transactional`, and on `Answer` create the `ConversationEntity` (owner = `userId`), add user and assistant `ChatMessageEntity`s, set `lastActivityAt`, save once, return `Answered`; the switch on `Response` is exhaustive (`Unavailable` → `AssistantUnavailable`, `NoAnswer` → `NoAnswer`, nothing written)
- [ ] T017 [P] [US1] Create `api/request/SendChatMessageRequest.java` (`UUID conversationId`, `String message`) and `api/responses/ChatAnswerResponse.java` (`UUID conversationId`, `String answer`)
- [ ] T018 [US1] Create `api/controllers/ChatController.java` (`@RestController`, `@RequestMapping("/api/private/chat")`, conditional on the flag): `@PostMapping("/messages")` builds the `Command` with `SessionContextHolder.getSession().id()` and switches exhaustively on `Result` per contracts/chat-api.md (200 `ChatAnswerResponse`; 400 `ErrorResponse.of(CHAT_*, correlationId)` for every error variant)
- [ ] T019 [P] [US1] Create `src/test/java/dev/leocamacho/demo/tests/api/paths/ChatApi.java` (REST-Assured wrapper over `ApiVerbs`, `sendMessage(UUID conversationId, String message)` with the bearer token from `ApiContext`) and register its path in `api/paths/Path.java` if the project lists paths there
- [ ] T020 [US1] Create `src/test/resources/features/Chatbot.feature` and `api/steps/ChatSteps.java` with an `@chatbot-live` scenario: registered and logged-in user sends "Hello" → 200, non-empty answer, conversation id is a UUID (reuse `AuthenticationSteps`/`ScenarioContext` for login)

**Checkpoint**: US1 works end to end with the flag on and a real key (quickstart.md §3, first request).

## Phase 4: User Story 2 - Continue a conversation with context (P1)

**Goal**: follow-up messages in an existing conversation reach the model with the earlier turns; other users'
conversations behave as non-existent.
**Independent Test**: "My name is Ana" then, with the returned id, "What is my name?" → the answer mentions Ana; the
same id used by another user → 400 `CHAT_CONVERSATION_NOT_FOUND`.

### Tests for User Story 2

- [ ] T021 [P] [US2] Extend `handlers/commands/SendChatMessageHandlerTests.java`: existing conversation → history loaded via `findByConversationIdOrderByCreatedAtDescIdDesc` with `Limit.of(historyMaxTurns)`, reversed to chronological order, followed by the new user turn; new turns appended to the existing conversation and `lastActivityAt` updated; unknown id or another owner (`findByIdAndOwnerId` empty) → `ConversationNotFound` with no provider call and no save; history longer than the cap → only the most recent `historyMaxTurns` turns sent

### Implementation for User Story 2

- [ ] T022 [US2] In `handlers/commands/impl/SendChatMessageHandlerImpl.java`: when `conversationId` is present, load it with `findByIdAndOwnerId(conversationId, userId)` (empty → `ConversationNotFound`), load the last `chatbot.history-max-turns` messages, reverse them, map `ChatRole` → `ChatProvider.Role`, and append the new turns to that conversation instead of creating one (inject the cap with `@Value("${chatbot.history-max-turns}")`)
- [ ] T023 [US2] Add `@chatbot-live` scenarios to `src/test/resources/features/Chatbot.feature` (steps in `api/steps/ChatSteps.java`): follow-up uses the earlier turn (answer contains the name given before); a second registered user using the first user's conversation id → 400 with code 3002; a random UUID → 400 with code 3002

**Checkpoint**: US1 and US2 pass together.

## Phase 5: User Story 3 - Operator turns the feature on or off (P1)

**Goal**: flag off by default; off → the route answers 404 and the app starts without a key; on without a key →
startup fails naming the setting.
**Independent Test**: start without flag and key → app starts, authenticated POST → 404 code 1004; start with
`FEATURES_CHATBOT_ENABLED=true` and no key → startup fails with a message naming `GOOGLE_AI_API_KEY`.

### Tests for User Story 3

- [ ] T024 [P] [US3] Create `api/GlobalExceptionHandlerTests.java`: a `NoResourceFoundException` (and a `NoHandlerFoundException`) passed to the new handler method → 404 `ErrorResponse` with code 1004 (stub the session via `SessionContextHolder` as the existing code requires); first confirm with a quick run which of the two exceptions Spring MVC 7 raises for an unmapped `/api/private/**` route and keep the assertion on that one at least (research R4)
- [ ] T025 [P] [US3] Add to `providers/GeminiChatProviderTests.java`: constructing the provider with a blank or null key throws `IllegalStateException` whose message contains `google.ai.api-key` and `GOOGLE_AI_API_KEY` and does not contain any key value

### Implementation for User Story 3

- [ ] T026 [US3] Add `@ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})` to `api/GlobalExceptionHandler.java` returning `ResponseEntity.status(NOT_FOUND).body(ErrorResponse.of(RESOURCE_NOT_FOUND, correlationId))`, declared so it wins over the `Throwable` handler
- [ ] T027 [US3] In `providers/gemini/GeminiChatProvider.java` constructor: if `google.ai.api-key` is null or blank, throw `IllegalStateException("google.ai.api-key (GOOGLE_AI_API_KEY) must be set when features.chatbot.enabled=true")`
- [ ] T028 [US3] Add a `@chatbot-disabled` scenario to `src/test/resources/features/Chatbot.feature`: logged-in user posts a message → 404 with code 1004; create `src/test/resources/junit-platform.properties` with `cucumber.filter.tags=not @chatbot-live`, and make the `e2eTest` task in `build.gradle.kts` forward `cucumber.filter.tags` (add it to the `listOf("host", "port")` forwarding list)

**Checkpoint**: `./gradlew e2eTest` against an app started with defaults runs only `@chatbot-disabled` and the
existing scenarios, and passes — this is what CI runs.

## Phase 6: User Story 4 - Clear errors when something goes wrong (P2)

**Goal**: invalid messages and AI failures return specific codes without leaking internals.
**Independent Test**: blank message → 400 code 3001 with no provider call; simulated 429 → 400 code 3003 with no key
or raw error in the body and nothing saved.

### Tests for User Story 4

- [ ] T029 [P] [US4] Extend `handlers/commands/SendChatMessageHandlerTests.java`: null, blank and 4,001-character messages → `InvalidMessage` with no repository or provider interaction; 4,000 characters accepted; `Unavailable` → `AssistantUnavailable` and `NoAnswer` → `NoAnswer`, both with no `save` (also for a new conversation: nothing created)
- [ ] T030 [P] [US4] Extend `providers/GeminiChatProviderTests.java`: 400 with `API_KEY_INVALID`, 401 and 403 → `Unavailable(AUTH)`; 429 → `Unavailable(QUOTA)`; 500/503 → `Unavailable(SERVICE_ERROR)`; I/O timeout (`ResourceAccessException`) → `Unavailable(TIMEOUT)`; `promptFeedback.blockReason` or `finishReason = SAFETY` → `NoAnswer(BLOCKED)`; no candidates or no text parts → `NoAnswer(EMPTY)`

### Implementation for User Story 4

- [ ] T031 [US4] In `handlers/commands/impl/SendChatMessageHandlerImpl.java`: validate before any I/O — null/blank → `InvalidMessage("message is required")`, longer than `MAX_MESSAGE_LENGTH = 4_000` (private static final constant) → `InvalidMessage("message is too long")`
- [ ] T032 [US4] In `providers/gemini/GeminiChatProvider.java`: map HTTP errors with `RestClient` `onStatus`/exception handling and timeouts to `Unavailable(reason)` and blocked/empty candidates to `NoAnswer(reason)` per T030; log `model`, status code and reason at WARN, never bodies or the key
- [ ] T033 [US4] Add `@chatbot-live` scenarios to `src/test/resources/features/Chatbot.feature`: blank message → 400 code 3001; message of 4,001 characters → 400 code 3001 (use `{String:4001}`)

**Checkpoint**: all stories pass; error bodies never contain the key.

## Phase 7: Polish & Cross-Cutting

- [ ] T034 Run `./gradlew build` and fix every Checkstyle finding (line length 120, `NeedBraces`, `MagicNumber`, constants `UPPER_SNAKE_CASE`, no star or unused imports, newline at EOF); report any warning
- [ ] T035 Verify the `gemini-flash-latest` alias against the live API (research R2); if it does not resolve, set the current stable Flash model in `src/main/resources/application.yaml` and in `specs/011-gemini-chatbot/contracts/chat-api.md`
- [ ] T036 Run quickstart.md end to end: defaults → 404; flag on without key → startup fails; flag on with key → two-turn conversation; `./gradlew e2eTest` and `./gradlew e2eTest "-Dcucumber.filter.tags=@chatbot-live"`
- [ ] T037 [P] Grep the codebase, test output and app log for the key value and for `?key=` to confirm the key never appears (SC-004)
- [ ] T038 [P] Document the chatbot settings (flag, `GOOGLE_AI_API_KEY`, model) and the `@chatbot-live` tag in `CLAUDE.md` (Security/Testing sections)
- [ ] T039 Write the PR description calling out: the new ArchUnit rule and why; unknown routes now return 404 instead of 400; no new dependencies; `Closes #11`

## Dependencies & Execution Order

### Phase dependencies

- **Setup (T001–T002)** → **Foundational (T003–T011)** → user stories → **Polish (T034–T039)**.
- **US1 (Phase 3)** depends only on Foundational. It is the MVP.
- **US2 (Phase 4)** extends the handler written in US1 (T016) — start it after T016.
- **US3 (Phase 5)**: T024/T026 (404) are independent of US1 and can start right after Foundational; T025/T027
  need the provider from T015.
- **US4 (Phase 6)** extends the handler (T016) and the provider (T015).

### Within each story

- Tests first, and they must fail before the implementation task.
- Handler and provider tasks touch the same files across stories (`SendChatMessageHandlerImpl.java`,
  `GeminiChatProvider.java`, `SendChatMessageHandlerTests.java`, `GeminiChatProviderTests.java`,
  `Chatbot.feature`): those tasks are sequential even across stories.

### Parallel opportunities

- T002 alongside T001.
- T004–T011 together (different files) once T003 is written.
- In US1: T012, T013, T014, T017, T019 together; then T015 → T016 → T018 → T020.
- US3's T024 + T026 in parallel with all of US1.
- In US4: T029 and T030 together.
- Polish: T037 and T038 together.

## Parallel Example: User Story 1

```text
T012 GeminiChatProviderTests (request shape, text answer)
T013 SendChatMessageHandlerTests (new conversation)
T014 GeminiDtos
T017 SendChatMessageRequest + ChatAnswerResponse
T019 ChatApi (Cucumber REST wrapper)
```

## Implementation Strategy

1. **MVP**: Phases 1–3 (T001–T020) → a signed-in user gets answers. Validate with quickstart.md §3 (first request).
2. **Context**: Phase 4 → real multi-turn conversations, ownership enforced.
3. **Safe rollout**: Phase 5 → flag defaults off, CI green without a key. Do not merge before this phase: CI's e2e
   job starts the app with defaults and needs `@chatbot-disabled` plus the tag filter.
4. **Hardening**: Phase 6 → specific error codes, no leaks.
5. **Polish**: Phase 7 → green build, model alias check, docs, PR.

Commit per coherent unit (Conventional Commits): `feat:` per story, `test:` for test-only additions, `docs:` for
CLAUDE.md, `build:` for the `e2eTest` change.
