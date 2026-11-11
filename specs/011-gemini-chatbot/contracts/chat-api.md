# Contract: Chat API

**Feature**: [../spec.md](../spec.md)

Available only when `features.chatbot.enabled=true`. Requires `Authorization: Bearer <jwt>`.

## `POST /api/private/chat/messages`

### Request (`api.request.SendChatMessageRequest`)

```json
{
  "conversationId": "3f8a2c1e-6b0d-4a7e-9a51-2d3c4b5a6f70",
  "message": "What is my name?"
}
```

| Field            | Type          | Required | Notes                                                |
|------------------|---------------|----------|------------------------------------------------------|
| `conversationId` | UUID \| null  | no       | Omit or `null` to start a new conversation           |
| `message`        | string        | yes      | Non-blank, at most 4,000 characters                  |

### 200 OK (`api.responses.ChatAnswerResponse`)

```json
{
  "conversationId": "3f8a2c1e-6b0d-4a7e-9a51-2d3c4b5a6f70",
  "answer": "Your name is Ana."
}
```

### Errors (`api.responses.ErrorResponse`)

| HTTP | `code` | `ErrorCode`                   | When                                                       |
|------|--------|-------------------------------|------------------------------------------------------------|
| 400  | 3001   | `CHAT_INVALID_MESSAGE`        | `message` missing, blank or longer than 4,000 characters   |
| 400  | 3002   | `CHAT_CONVERSATION_NOT_FOUND` | Unknown `conversationId`, or it belongs to another user    |
| 400  | 3003   | `CHAT_ASSISTANT_UNAVAILABLE`  | AI service rejected the key, timed out, quota, other error |
| 400  | 3004   | `CHAT_NO_ANSWER`              | AI service returned an empty or safety-blocked answer      |
| 403  | —      | —                             | Missing or invalid JWT (existing behavior of every `/api/private/**` route) |
| 404  | 1004   | `RESOURCE_NOT_FOUND`          | Feature flag off (route not mapped), as any unknown route  |

Example:

```json
{
  "message": "The assistant is unavailable",
  "code": 3003,
  "correlationId": "9b2e4f61-1c7a-4d3e-8f05-6a7b8c9d0e1f",
  "params": []
}
```

Error bodies never contain the API key or the AI service's raw error.

## Configuration contract (`application.yaml`)

```yaml
features:
  chatbot:
    enabled: false              # FEATURES_CHATBOT_ENABLED
google:
  ai:
    api-key: ${GOOGLE_AI_API_KEY:}
    model: gemini-flash-latest
    base-url: https://generativelanguage.googleapis.com/v1beta
    timeout: 30s
chatbot:
  history-max-turns: 20
```

| Flag    | Key set | Startup                                                         |
|---------|---------|-----------------------------------------------------------------|
| `false` | any     | Starts; chat route answers 404                                  |
| `true`  | yes     | Starts; chat route available                                    |
| `true`  | no      | Fails with a message naming `google.ai.api-key` / `GOOGLE_AI_API_KEY` |
