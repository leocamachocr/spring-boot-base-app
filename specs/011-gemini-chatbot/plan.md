# Implementation Plan: Conversational Chatbot (Stage 1: Plain Chat)

**Branch**: `speckit/011-gemini-chatbot` | **Date**: 2026-11-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/011-gemini-chatbot/spec.md` (GitHub issue #11)

## Summary

Signed-in users chat with a Gemini-backed assistant through `POST /api/private/chat/messages`. The server keeps
each conversation and its turns in two new tables and sends the most recent turns to Gemini as context. A
`SendChatMessageHandler` command orchestrates validation, ownership, the provider call (outside any transaction)
and the final save. Gemini is reached over its REST API with Spring's `RestClient` behind a `ChatProvider`
interface, so no new dependency is added and no Gemini type leaves `providers`. Everything is gated by
`features.chatbot.enabled` (default off) through `@ConditionalOnProperty`. Stage 2 (tools over the user's tasks) is
prepared with three thin seams: a `tools` list on the provider request (empty now), a `kind` column on messages,
and a `ChatTool` interface with no implementations.

## Technical Context

**Language/Version**: Java 26
**Primary Dependencies**: Spring Boot 4.1.1 (Web MVC, Data JPA, Security 7.1, Liquibase), Spring `RestClient` +
Jackson 3 (already present). No new dependencies (research R1).
**Storage**: H2 via Spring Data JPA; new Liquibase changeset 3 (`004-create-chat-tables.sql`)
**Testing**: JUnit 5 + Mockito (handler), `MockRestServiceServer` (provider), ArchUnit, Cucumber E2E
(`./gradlew e2eTest`)
**Target Platform**: JVM server (local Windows dev, Ubuntu CI)
**Project Type**: Web service (single Spring Boot project)
**External service**: Gemini API `v1beta` `generateContent`, key in `x-goog-api-key` header (R1, R2)
**Performance Goals**: 95% of answers in under 10 s (SC-001); HTTP timeout to Gemini 30 s (`google.ai.timeout`)
**Constraints**: API key never logged, returned or committed; DB transactions never span the Gemini call (R6);
message ≤ 4,000 characters; at most 20 recent turns sent as context (configurable)
**Scale/Scope**: 1 endpoint, 1 command handler, 1 provider, 2 entities, 1 changeset

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|-----------|-------|--------|
| I. Reference Architecture | Packages: `api` (controller, request, response), `handlers.commands` (+ `impl`), `handlers.tools`, `providers` (`ChatProvider` + `providers.gemini` impl), `persistence` (2 entities, 2 repositories), `exception` (`ErrorCode`). Gemini behind a `Provider` interface (ARCHITECTURE.md §1.6, §5). UUID ids (§6). Explicit entity → `Result` mapping (§3). | PASS |
| II. CQRS Use Cases | One command `SendChatMessageHandler` with nested `Command` and sealed `Result`; impl `@Component`, one public method. Controller builds the `Command`, switches exhaustively on `Result` (§3, §4). | PASS |
| III. Results first | Invalid message, conversation not found, assistant unavailable, no answer are `Result` variants; provider failures are `ChatProvider.Response` variants, not exceptions. No `BusinessException` needed (nothing to roll back: the single save happens last). | PASS |
| IV. Enforced layering | `api` → `handlers` → `persistence`; handlers reach Gemini only through `ChatProvider`. New ArchUnit rule: nothing outside `providers..` depends on `providers.gemini..` (added in this PR, reason stated in the PR). | PASS |
| V. Tests | Handler unit tests, provider tests with `MockRestServiceServer`, 404 test, `Chatbot.feature` (R9). | PASS |
| VI. Green build | `./gradlew build` must pass; no `TODO`s; magic numbers as constants/properties. | PASS |
| VII. Simplicity | No new dependency (R1, R3, R5). Tool seams kept to what FR-014/FR-015 require; justified below. | PASS (justified) |
| Tech constraints | New changeset only, existing ones untouched; route under `/api/private/**`; user from `SessionContextHolder.getSession().id()`. Security change: none to authentication; **behavior change**: unknown routes now answer 404 instead of 400 (R4), called out in the PR. | PASS |

**Post-design re-check (after Phase 1)**: unchanged — PASS. The design adds no layer, package type or dependency
beyond the table above.

## Project Structure

### Documentation (this feature)

```text
specs/011-gemini-chatbot/
├── spec.md
├── plan.md              # This file
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   └── chat-api.md      # Phase 1
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks, not created here)
```

### Source Code (repository root)

```text
src/main/java/dev/leocamacho/demo/
├── api/
│   ├── GlobalExceptionHandler.java              # + 404 for NoResourceFound/NoHandlerFound (R4)
│   ├── controllers/ChatController.java          # NEW, @ConditionalOnProperty, POST /api/private/chat/messages
│   ├── request/SendChatMessageRequest.java      # NEW
│   └── responses/ChatAnswerResponse.java        # NEW
├── exception/ErrorCode.java                     # + RESOURCE_NOT_FOUND 1004, CHAT_* 3001–3004
├── handlers/
│   ├── commands/SendChatMessageHandler.java     # NEW contract (Command + sealed Result)
│   ├── commands/impl/SendChatMessageHandlerImpl.java  # NEW, @ConditionalOnProperty
│   └── tools/ChatTool.java                      # NEW seam, no implementations (R7)
├── providers/
│   ├── ChatProvider.java                        # NEW contract (Request, Turn, ToolDefinition, sealed Response)
│   └── gemini/
│       ├── GeminiChatProvider.java              # NEW, @ConditionalOnProperty, RestClient, key check (R5)
│       └── GeminiDtos.java                      # NEW, package-private request/response records
└── persistence/
    ├── model/ConversationEntity.java            # NEW
    ├── model/ChatMessageEntity.java             # NEW (+ ChatRole, ChatMessageKind enums)
    ├── repositories/ConversationRepository.java # NEW
    └── repositories/ChatMessageRepository.java  # NEW

src/main/resources/
├── application.yaml                             # + features.chatbot, google.ai, chatbot.history-max-turns
└── db/changelog/
    ├── 004-create-chat-tables.sql               # NEW
    └── db.changelog-master.xml                  # + changeSet id="3"

src/test/java/dev/leocamacho/demo/tests/
├── handlers/commands/SendChatMessageHandlerTests.java   # NEW
├── providers/GeminiChatProviderTests.java               # NEW (MockRestServiceServer)
├── api/GlobalExceptionHandlerTests.java                 # NEW (unknown route → 404)
├── api/paths/ChatApi.java                               # NEW REST-Assured wrapper
├── api/steps/ChatSteps.java                             # NEW
└── structure/StructureValidationTests.java              # + providers.gemini isolation rule

src/test/resources/
├── features/Chatbot.feature                     # NEW (@chatbot-disabled, @chatbot-live)
└── junit-platform.properties                    # NEW: cucumber.filter.tags=not @chatbot-live

build.gradle.kts                                 # e2eTest forwards -Dcucumber.filter.tags
```

**Structure Decision**: Single Spring Boot project following `docs/ARCHITECTURE.md` §2. The only new package
kinds are `providers` (mandated by the constitution, created now because this is the first external integration)
and `handlers.tools` (stage-2 seam). Test files sit next to the existing ones.

> This plan touches 13 new main files and 8 new test/resource files. Per the user's global rule ("more than 3
> new files → confirm first"), implementation starts only after this plan is approved on the issue
> (`speckit:approved`).

## Design Notes

### Request flow (`SendChatMessageHandlerImpl.handle`)

1. Validate `message` (null/blank/> `MAX_MESSAGE_LENGTH` = 4,000) → `InvalidMessage`. No I/O yet.
2. If `conversationId` is present: `conversationRepository.findByIdAndOwnerId(id, userId)`; empty →
   `ConversationNotFound`. Load the latest `historyMaxTurns` messages, reverse to chronological order. If absent:
   history is empty and the conversation is created later.
3. Stamp the user turn time; build `ChatProvider.Request(history + user turn, toolDefinitions)` where
   `toolDefinitions` comes from `ObjectProvider<ChatTool>` (empty in stage 1).
4. Call `chatProvider.reply(request)` — no transaction is open.
5. `switch` on `Response`: `Unavailable` → `AssistantUnavailable`; `NoAnswer` → `NoAnswer` (nothing written);
   `Answer` → create the conversation if new, append the user turn and the assistant turn, update
   `last_activity_at`, single `save`, return `Answered(conversationId, text)`.

### Gemini mapping (`GeminiChatProvider`)

- `Role.USER` → `"user"`, `Role.ASSISTANT` → `"model"`; each turn → `{role, parts:[{text}]}`.
- `tools` non-empty → `tools:[{functionDeclarations:[...]}]`; empty → field omitted.
- HTTP 400/401/403 → `Unavailable(AUTH)` when the error status is about the key, else `SERVICE_ERROR`; 429 →
  `QUOTA`; 5xx → `SERVICE_ERROR`; `ResourceAccessException` (timeout/IO) → `TIMEOUT`.
- `promptFeedback.blockReason` present or `finishReason == "SAFETY"` → `NoAnswer(BLOCKED)`; no text parts →
  `NoAnswer(EMPTY)`; otherwise concatenate the text parts → `Answer`.
- Logs: model, HTTP status and reason only. Never the key, never request/response bodies.

### Controller (`ChatController`)

`@RestController @RequestMapping("/api/private/chat")`, `@PostMapping("/messages")`. Reads
`SessionContextHolder.getSession()` for `id()` and `correlationId()`; maps `Result` exhaustively per
[contracts/chat-api.md](./contracts/chat-api.md).

## Open Decisions

- None blocking. To verify during implementation: the `gemini-flash-latest` alias (R2) and the exact exception
  Spring MVC 7 raises for an unmapped route (R4) — both covered by tests in the task list.

## Complexity Tracking

| Item | Why Needed | Simpler Alternative Rejected Because |
|------|------------|-------------------------------------|
| `ChatTool` interface with no implementations, and `ToolDefinition` list on `ChatProvider.Request` (always empty now) | FR-015 and the issue require that adding a task tool later is an addition, not a contract change | Leaving them out means stage 2 changes the provider contract and the handler's dependencies; the seams are ~20 lines with no behavior |
| `kind` column on `chat_messages` (always `TEXT` now) | FR-014: the record must hold future tool-call/result turns without reshaping stored conversations | Adding it later requires a migration over existing rows; the column costs nothing now |
| New ArchUnit rule for `providers.gemini` | FR-016 and Constitution IV (provider impls reached only through their interface) | Convention alone would let Gemini DTOs leak into `handlers` unnoticed |

Deliberately **not** built now (would be dead code, Constitution VII): a `ToolCallRequested` response variant, the
call → execute → reply loop, tool turn kinds in Java, conversation listing/deletion.
