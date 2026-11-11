# Data Model: Conversational Chatbot (Stage 1: Plain Chat)

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

## Persistence (Liquibase changeset 3: `004-create-chat-tables.sql`)

> `003-rename-tables.sql` exists but is not registered in the master changelog; it is left untouched and the new
> file uses the next free number. Existing changesets are never edited.

### `chat_conversations` → `ConversationEntity`

| Column             | Type        | Constraints                         | Notes                                   |
|--------------------|-------------|-------------------------------------|-----------------------------------------|
| `id`               | `UUID`      | PK                                  | `@GeneratedValue(strategy = UUID)`      |
| `owner_id`         | `UUID`      | NOT NULL, FK → `users(id)`, indexed | Session user id (`Session.id()`)        |
| `created_at`       | `TIMESTAMP` | NOT NULL                            |                                         |
| `last_activity_at` | `TIMESTAMP` | NOT NULL                            | Updated on every answered message       |

### `chat_messages` → `ChatMessageEntity`

| Column            | Type          | Constraints                                       | Notes                                      |
|-------------------|---------------|---------------------------------------------------|--------------------------------------------|
| `id`              | `UUID`        | PK                                                |                                            |
| `conversation_id` | `UUID`        | NOT NULL, FK → `chat_conversations(id)`           | `@ManyToOne(fetch = LAZY)`                 |
| `role`            | `VARCHAR(16)` | NOT NULL                                          | `USER` \| `ASSISTANT` (enum as string)     |
| `kind`            | `VARCHAR(32)` | NOT NULL                                          | Stage 1: always `TEXT`                     |
| `content`         | `CLOB`        | NOT NULL                                          | `TEXT`: plain text. Later kinds: JSON      |
| `created_at`      | `TIMESTAMP(9)`| NOT NULL                                          | Ordering key, with `id` as tie-breaker     |

Index: `(conversation_id, created_at)`.

**Readiness (FR-014)**: stage 2 adds `TOOL_CALL` and `TOOL_RESULT` values to `kind` (and possibly a `TOOL` role),
storing the call/result as JSON in `content`. No column is added and no existing row changes.

### Repositories (`persistence.repositories`, no `@Repository`)

- `ConversationRepository extends JpaRepository<ConversationEntity, UUID>`
  - `Optional<ConversationEntity> findByIdAndOwnerId(UUID id, UUID ownerId)` — ownership check (FR-005).
- `ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID>`
  - `List<ChatMessageEntity> findByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId, Limit limit)` —
    most recent turns; the handler reverses them to chronological order.

## Validation rules

| Rule                                    | Where                         | Outcome                          |
|-----------------------------------------|-------------------------------|----------------------------------|
| Message null, blank                     | Handler, before any I/O       | `Result.InvalidMessage`          |
| Message longer than 4,000 characters    | Handler (`MAX_MESSAGE_LENGTH`)| `Result.InvalidMessage`          |
| Conversation id unknown or not owned    | Handler (`findByIdAndOwnerId`)| `Result.ConversationNotFound`    |

## Handler contract (`handlers`, no persistence types)

`SendChatMessageHandler` (command), with `SendChatMessageHandlerImpl` in `handlers.commands.impl`:

- `Command(UUID userId, UUID conversationId /* null = new */, String message)`
- `sealed Result`:
  - `Answered(UUID conversationId, String answer)`
  - `InvalidMessage(String reason)`
  - `ConversationNotFound()`
  - `AssistantUnavailable()`
  - `NoAnswer()`

## Provider contract (`providers`, no Gemini types)

`ChatProvider`:

- `Response reply(Request request)`
- `Request(List<Turn> history, List<ToolDefinition> tools)` — `tools` is empty in stage 1.
- `Turn(Role role, String text)`; `enum Role { USER, ASSISTANT }`
- `ToolDefinition(String name, String description, Map<String, Object> parameters)` — JSON-schema parameters.
- `sealed Response`:
  - `Answer(String text)`
  - `Unavailable(Reason reason)` — `enum Reason { AUTH, QUOTA, TIMEOUT, SERVICE_ERROR }` (for logs only)
  - `NoAnswer(NoAnswerReason reason)` — `enum NoAnswerReason { EMPTY, BLOCKED }`

## Tool seam (`handlers.tools`, no implementations in stage 1)

`ChatTool`:

- `String name()`, `String description()`, `Map<String, Object> parameters()`
- `Map<String, Object> execute(UUID userId, Map<String, Object> arguments)` — runs on behalf of the given user only.

## State transitions

```text
(no conversation) --first answered message--> Conversation[active]
Conversation[active] --answered message--> Conversation[active] (last_activity_at updated, +2 turns)
any --provider failure / invalid message--> unchanged (nothing written)
```
