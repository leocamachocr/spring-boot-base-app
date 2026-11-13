# Research: Conversational Chatbot (Stage 1: Plain Chat)

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-11-11

Each entry records a decision, why it was taken, and what else was considered.

## R1. How to call Gemini: REST via Spring `RestClient` vs `google-genai` SDK vs Spring AI

- **Decision**: Call the Gemini API REST endpoint (`POST {base-url}/models/{model}:generateContent`, key in the
  `x-goog-api-key` header) with Spring's `RestClient`, from a provider implementation in
  `providers.gemini`. No new dependency.
- **Rationale**:
  - Constitution VII: no new dependency without a spec requirement that needs it. `RestClient` and Jackson are
    already on the classpath through `spring-boot-starter-web`.
  - The request/response surface needed now (and in stage 2) is small: `contents[]` with `role` + `parts[]`,
    `tools[].functionDeclarations[]`, `candidates[].content.parts[]` (`text` or `functionCall`),
    `finishReason`, `promptFeedback.blockReason`. A handful of package-private records cover it.
  - The `google-genai` SDK brings its own HTTP stack and Jackson 2 (`com.fasterxml`), while Spring Boot 4 runs on
    Jackson 3 (`tools.jackson`): two JSON stacks side by side for one endpoint. Its version could not be pinned
    with confidence during research (sources disagreed between 1.36 and 1.64).
  - Tests can stub the HTTP layer with `MockRestServiceServer` (already in `spring-boot-starter-test`), so the
    provider's mapping of errors and safety blocks is unit-tested without calling Google.
- **Alternatives considered**:
  - `com.google.genai:google-genai`: typed API and automatic function calling, but extra dependencies, a second
    Jackson, and its types would have to be kept inside `providers` anyway. Worth revisiting only if stage 2 needs
    features that are costly to hand-write (streaming, live API).
  - Spring AI (`ChatClient` with Google GenAI): brings tool calling out of the box, but it is a whole framework
    with its own abstractions over the ones the constitution already mandates (handlers + `Provider`), and it
    would make tool execution happen inside the framework instead of in our handler.

## R2. Model and endpoint configuration

- **Decision**: `google.ai.model` in `application.yaml`, default `gemini-flash-latest` (Google's alias for the
  current Flash model); `google.ai.base-url` default `https://generativelanguage.googleapis.com/v1beta`;
  `google.ai.timeout` default `30s`. The API key comes from `GOOGLE_AI_API_KEY` (`google.ai.api-key:
  ${GOOGLE_AI_API_KEY:}`), never committed.
- **Rationale**: Model names in Google's docs change often (pages show several Flash versions); an alias keeps the
  default working, and the operator can pin an exact model. The key goes in a header, not in the URL, so it never
  appears in access logs.
- **To verify at implementation**: that the `gemini-flash-latest` alias still resolves on the v1beta endpoint; if
  not, pin the current stable Flash model from the Gemini models page.
- **Verified 2026-11-11 with a real key**: the alias resolves, but it answered `503 UNAVAILABLE` ("high demand") on
  every attempt, while `gemini-3.5-flash` answered in under 2 s; `gemini-2.5-flash` is no longer available to new
  keys. **Revised decision**: default `google.ai.model: gemini-3.5-flash`, pinned; still overridable.
- **Alternatives considered**: hardcoding a model version (goes stale); key as `?key=` query parameter (leaks in
  logs and proxies).

## R3. Feature flag mechanism

- **Decision**: Property `features.chatbot.enabled` (default `false` in `application.yaml`; `FEATURES_CHATBOT_ENABLED`
  works through Spring's relaxed binding). The chatbot beans — controller, handler impl and Gemini provider — carry
  `@ConditionalOnProperty(name = "features.chatbot.enabled", havingValue = "true")`.
- **Rationale**: Simplest mechanism that meets FR-009/FR-010: with the flag off the beans do not exist, so the
  route is not mapped and nothing reads the API key. No flag library is needed for an on/off switch decided at
  startup.
- **Alternatives considered**: Togglz/Unleash/OpenFeature (runtime toggling, not required, new dependency); a
  runtime `if (enabled)` check in the controller (beans would still need a key or a null-safe provider).

## R4. "Not found" when the flag is off

- **Finding**: An unmapped route under `/api/private/**` behaves like this today:
  - Unauthenticated: the security chain answers 403 (verified) before routing — same as any other private route, so the
    chatbot route is indistinguishable from a non-existent one.
  - Authenticated: Spring MVC raises `NoResourceFoundException` (or `NoHandlerFoundException`), and
    `GlobalExceptionHandler.handleException(Throwable)` turns it into **400 `UNKNOWN_ERROR`**, not 404.
- **Decision**: Add an `@ExceptionHandler` for `NoResourceFoundException` and `NoHandlerFoundException` in the
  existing `GlobalExceptionHandler` that returns **404** with `ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, ...)`
  (new code `1004`). This makes every unknown route answer 404, the chatbot route with the flag off included.
- **Impact**: Behavior change for all unknown routes (400 → 404). It is a correction, but it must be called out in
  the PR description. Confirm the exact exception type with a test during implementation.
- **Alternatives considered**: a dedicated "disabled" controller returning 404 when the flag is off (more code,
  and unknown routes would still answer 400).

## R5. Failing at startup when the flag is on and the key is missing

- **Decision**: The Gemini provider's constructor checks the key; if blank it throws `IllegalStateException`
  with a message naming the setting (`google.ai.api-key` / `GOOGLE_AI_API_KEY`) and never its value. The bean only
  exists when the flag is on, so the check only runs then.
- **Rationale**: No Bean Validation starter is on the classpath; adding one for a single check would break
  Constitution VII.
- **Alternatives considered**: `@ConfigurationProperties` + `@Validated @NotBlank` (needs
  `spring-boot-starter-validation`).

## R6. Where the history lives and how it is persisted

- **Decision** (from the spec clarification of 2026-11-11): in the database, per conversation. Two tables,
  `chat_conversations` and `chat_messages` (Liquibase changeset 3). Each message has `role`, `kind` and `content`;
  in stage 1 `kind` is always `TEXT` and `content` is the plain text. Future kinds (`TOOL_CALL`, `TOOL_RESULT`)
  will store a JSON payload in the same `content` column, so stage 2 adds values, not columns (FR-014).
- **Transaction boundary**: the call to Gemini (up to 30 s) runs **outside** any transaction. The handler reads the
  conversation and its recent turns, calls the provider, and only on success saves, in one `save`, the user turn
  and the assistant turn (and the conversation itself when new). On failure nothing is written: the user's
  message is not recorded as an answered turn, and a new conversation is not created.
- **Ordering and concurrency**: turns are ordered by `created_at` (timestamp with fractional seconds), then by
  `id`. The user turn is stamped before the provider call and the assistant turn after it, so each pair stays in
  order; concurrent messages to one conversation both get saved (no row is updated in place except
  `last_activity_at`, which is last-write-wins and harmless).
- **Context window**: the provider receives at most `chatbot.history-max-turns` most recent turns (default 20,
  a named constant/property, not a magic number). Older turns stay in the database.

## R7. Shape of the tool-ready contracts (FR-014–FR-016) without speculative code

- **Decision**:
  - `ChatProvider` (in `providers`) takes a `Request(List<Turn> history, List<ToolDefinition> tools)`; stage 1
    always passes an empty `tools` list. `Turn` has a `Role` (`USER`, `ASSISTANT`) and a text. `ToolDefinition`
    is a record (name, description, JSON-schema parameters as `Map<String, Object>`).
  - `ChatProvider.Response` is a sealed interface with `Answer(String text)`, `Unavailable(Reason reason)` and
    `NoAnswer(Reason reason)`. A `ToolCallRequested` variant is **not** added now: nothing could produce it while
    `tools` is empty. Stage 2 adds it, and because `Response` is sealed the compiler points at every `switch`
    that must handle it — an additive, compiler-guided change.
  - `ChatTool` (in `handlers.tools`): name, description, parameters schema, and
    `Map<String, Object> execute(UUID userId, Map<String, Object> arguments)`. No implementations. The handler
    collects `ChatTool` beans through `ObjectProvider<ChatTool>` (empty in stage 1) and maps them to
    `ToolDefinition`s. The explicit `userId` makes every tool run only on the signed-in user's data (FR-015).
  - Gemini DTOs are package-private in `providers.gemini`; an ArchUnit rule forbids anything outside
    `providers..` from depending on `providers.gemini..` (FR-016).
- **Rationale**: These are the minimum seams the spec requires (FR-014–FR-016) and the issue asked for
  explicitly; each costs a few lines. The parts that would only be dead code now (tool-call response variant, the
  call → execute → reply loop, tool turn kinds in Java) are deferred to stage 2. See Complexity Tracking in the
  plan.
- **Alternatives considered**: no seams at all (stage 2 would reshape the provider contract and the message
  table); full tool loop now with zero tools (dead code, violates Constitution VII).

## R8. HTTP contract and error codes

- **Decision**: `POST /api/private/chat/messages` with `{ "conversationId": uuid|null, "message": string }` →
  `200 { "conversationId": uuid, "answer": string }`. Expected errors follow the project convention (400 with
  `ErrorResponse` and a specific code):
  - `CHAT_INVALID_MESSAGE` (3001): message missing, blank, or over 4,000 characters.
  - `CHAT_CONVERSATION_NOT_FOUND` (3002): unknown id or another user's conversation (same answer for both).
  - `CHAT_ASSISTANT_UNAVAILABLE` (3003): invalid key, timeout, quota, or any Gemini error.
  - `CHAT_NO_ANSWER` (3004): empty answer or answer blocked for safety.
  - `RESOURCE_NOT_FOUND` (1004): unknown route (R4).
- **Rationale**: CLAUDE.md fixes "expected errors → 400 with `ErrorResponse.of(...)`"; clients tell cases apart by
  `code`. Error bodies carry only the code, message and correlation id — never Gemini's raw error or the key.
- **Alternatives considered**: 404 for an unknown conversation and 503 for provider failures (more RESTful, but
  breaks the project's single convention for expected outcomes; can be revisited project-wide).

## R9. Testing strategy

- **Decision**:
  - Unit (JUnit 5 + Mockito): `SendChatMessageHandlerImpl` with mocked repositories and `ChatProvider` — new and
    existing conversation, ownership, validation, provider failure writes nothing, history cap.
  - Unit: `GeminiChatProvider` against `MockRestServiceServer` — request shape (roles `user`/`model`, header, no
    key in URL), text answer, 400/401/403/429/5xx/timeout → `Unavailable`, empty or `SAFETY` → `NoAnswer`.
  - Unit: `GlobalExceptionHandler` returns 404 for unknown routes.
  - ArchUnit: new rule for `providers.gemini` isolation.
  - Cucumber (`Chatbot.feature`): `@chatbot-disabled` scenario (authenticated POST → 404) runs by default and in CI,
    where the flag is off. Scenarios that need the flag on and a real key are tagged `@chatbot-live` and excluded
    by default through `cucumber.filter.tags=not @chatbot-live` in `src/test/resources/junit-platform.properties`;
    the `e2eTest` task forwards `-Dcucumber.filter.tags` so they can be run on demand.
- **Rationale**: Constitution V (handler tests + Cucumber for new endpoints) without CI ever calling Google or
  needing a secret.
