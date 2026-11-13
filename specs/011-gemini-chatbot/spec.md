# Feature Specification: Conversational Chatbot (Stage 1: Plain Chat)

**Feature Branch**: `speckit/011-gemini-chatbot`
**Created**: 2026-11-11
**Status**: Draft
**Input**: GitHub issue #11 — "Chatbot conversacional simple con Gemini (Google AI Studio API key)"

## Context

Authenticated users of the application get a chatbot they can hold a plain text conversation with. The answers
come from an external generative AI service (Google's Gemini models, reached with a Google AI Studio key).

This is **stage 1** of the chatbot. Later stages will let the assistant act on the user's tasks: list them, add
new ones and search them. Stage 1 delivers no such actions, but it must be shaped so that adding them later is an
addition, not a rework of the conversation feature built now.

The whole feature sits behind an operator-controlled switch and is off unless an operator turns it on.

## Clarifications

### Session 2026-11-11

- Q: Where does the conversation history live? → A: On the server, stored durably per conversation; the
  client sends only the new message and the conversation identifier.

### Session 2026-11-11 (extension: calculator tool)

- Scope extension requested on the issue: the assistant gets a first action, a calculator for addition,
  subtraction, multiplication, division and square root, invoked through the AI service's tool calling.
- Q: Should the answer show the calculator calls the assistant made? → A: Yes. The answer lists each call (name,
  arguments, result), and the calls are also kept in the conversation history.
- Q: One tool or one per operation? → A: One `calculate` tool with an `operation` argument.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Send a message and get an answer (Priority: P1)

A signed-in user sends a text message to the chatbot and receives the assistant's text answer.

**Why this priority**: It is the core of the feature; without it there is no chatbot.

**Independent Test**: With the feature on, a signed-in user sends "Hello, what can you do?" and receives a
non-empty text answer.

**Acceptance Scenarios**:

1. **Given** the feature is on and the user is signed in, **When** the user sends a non-empty message,
   **Then** the user receives the assistant's text answer and an identifier for the conversation.
2. **Given** the feature is on, **When** someone who is not signed in sends a message, **Then** the request is
   rejected as unauthenticated and nothing is sent to the AI service.

---

### User Story 2 - Continue a conversation with context (Priority: P1)

The user sends a follow-up message in the same conversation, and the assistant's answer takes the earlier turns
into account.

**Why this priority**: A chatbot that forgets the previous message is not a conversation; the issue requires
multi-turn context.

**Independent Test**: The user says "My name is Ana", then in the same conversation asks "What is my name?",
and the answer mentions "Ana".

**Acceptance Scenarios**:

1. **Given** an existing conversation of the user, **When** the user sends a follow-up message in it,
   **Then** the assistant receives the earlier turns of that conversation and answers with that context.
2. **Given** a conversation that belongs to another user, **When** a user tries to continue it,
   **Then** the request is rejected as if the conversation did not exist, and none of its content is revealed.
3. **Given** a conversation identifier that does not exist, **When** the user sends a message to it,
   **Then** the user receives a "conversation not found" error.

---

### User Story 3 - Operator turns the feature on or off (Priority: P1)

An operator decides whether the chatbot is available, through configuration, without changing code.

**Why this priority**: The issue requires the feature to ship disabled and to be activated deliberately; the rest
of the application must keep working when it is off, including with no AI key configured.

**Independent Test**: Start the application with the switch off and no AI key: it starts, and the chatbot
route answers "not found". Start it with the switch on and a key: the chatbot answers.

**Acceptance Scenarios**:

1. **Given** the switch is off (the default), **When** any client calls the chatbot, **Then** the response is
   "not found", exactly as for a route that does not exist.
2. **Given** the switch is off and no AI key is configured, **When** the application starts, **Then** it starts
   normally and every other feature works.
3. **Given** the switch is on and no AI key is configured, **When** the application starts, **Then** startup
   fails with a message that names the missing setting (and never prints a key).

---

### User Story 4 - Clear errors when something goes wrong (Priority: P2)

When the user's message is invalid or the AI service fails, the user gets a clear, specific error instead of a
generic failure, and no internal details leak.

**Why this priority**: Needed for a usable feature, but secondary to the happy path.

**Independent Test**: Send an empty message and check the specific error; simulate an AI service failure and
check the specific error.

**Acceptance Scenarios**:

1. **Given** the feature is on, **When** the user sends an empty or blank message, or one longer than the limit,
   **Then** the user receives a validation error with a specific error code and nothing is sent to the AI service.
2. **Given** the AI service rejects the key, times out or reports its quota exhausted, **When** the user sends
   a message, **Then** the user receives an "assistant unavailable" error with a specific error code, the
   response contains neither the key nor the AI service's raw error, and the user's message is not recorded as an
   answered turn.

---

### User Story 5 - The assistant uses a calculator for arithmetic (Priority: P2)

When the user asks for an addition, subtraction, multiplication, division or square root, the assistant asks the
system to run a calculator with the right arguments, receives the result, and answers with it. The user sees
which calculations were run.

**Why this priority**: It is the first assistant action, proving the action loop end to end before the task
actions of later stages.

**Independent Test**: Ask "What is the square root of 144?": the answer says 12, and the response lists a
`calculate` call with operation `sqrt`, argument 144 and result 12.

**Acceptance Scenarios**:

1. **Given** the feature is on, **When** the user asks for an arithmetic operation, **Then** the assistant
   requests the calculator, the system runs it with the requested arguments, sends the result back to the
   assistant, and the user receives the final answer together with the list of calculator calls.
2. **Given** a division by zero or the square root of a negative number, **When** the calculator runs, **Then** it
   returns an error result instead of failing, and the assistant explains it to the user.
3. **Given** a conversation in which the calculator was used, **When** the user sends a follow-up message, **Then**
   the earlier calculator calls and results are part of the context the assistant receives.
4. **Given** the assistant keeps requesting actions without answering, **When** the limit of action rounds per
   message is reached, **Then** the user receives the "assistant could not answer" error and nothing is written.

---

### Edge Cases

- The AI service returns an empty answer or blocks the answer for safety reasons: the user receives an
  "assistant could not answer" error; the conversation stays usable.
- Two messages are sent to the same conversation at the same time: both are recorded in a consistent order;
  neither turn is lost.
- A very long conversation: the assistant receives the most recent turns that fit the context limit; older turns
  are kept in the record but may not be sent.
- The switch is turned off while conversations exist: conversations are kept, just not reachable until it is on
  again.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a signed-in user send a text message to the chatbot and receive the assistant's
  text answer.
- **FR-002**: The system MUST reject chatbot requests from users who are not signed in.
- **FR-003**: Sending a message without a conversation identifier MUST start a new conversation owned by the
  user; the answer MUST include that conversation's identifier.
- **FR-004**: Sending a message with a conversation identifier MUST continue that conversation, and the assistant
  MUST receive the earlier turns of it as context.
- **FR-005**: A user MUST only be able to use their own conversations; another user's conversation MUST behave
  as non-existent.
- **FR-006**: The server MUST keep each conversation's history durably (it survives application restarts); the
  client sends only the new message and the conversation identifier, never the history.
- **FR-007**: The system MUST reject empty, blank or over-limit messages (limit: 4,000 characters) with a specific
  validation error code, without contacting the AI service.
- **FR-008**: The system MUST report AI service failures (invalid key, timeout, quota exhausted, empty or blocked
  answer) as a specific "assistant unavailable" error, without exposing the key or the service's raw error.
- **FR-009**: The chatbot MUST be available only when an operator-controlled switch is on; the switch MUST be off
  by default and changeable through configuration or environment, without code changes.
- **FR-010**: With the switch off, the chatbot MUST behave as a non-existent route, and the application MUST start
  and run without an AI key.
- **FR-011**: With the switch on and no AI key, the application MUST refuse to start with a clear message.
- **FR-012**: The AI key and the model used MUST be configurable by the operator; the key MUST never be stored in
  the repository, logged, or returned to users.
- **FR-013**: The only action the assistant can perform in this stage is the calculator (FR-017); everything else
  is a text reply.
- **FR-014** (readiness for later stages): The conversation record MUST be able to hold, besides plain text turns,
  future turn kinds in which the assistant asks to perform an action and receives its result, so that adding
  actions later does not require reshaping existing conversations.
- **FR-015** (readiness for later stages): Adding an action for the assistant (such as listing, adding or
  searching the user's tasks) MUST be possible by adding that action alone, without changing how users send
  messages or read answers. Actions MUST run on behalf of, and only on the data of, the signed-in user.
- **FR-016**: The conversation feature MUST NOT depend on the specifics of one AI service, so that the service
  could be replaced without changing what users see.
- **FR-017**: The assistant MUST have a calculator action supporting addition, subtraction, multiplication,
  division and square root, and MUST be told to use it for arithmetic instead of computing by itself.
- **FR-018**: When the assistant requests an action, the system MUST run it with the requested arguments, return
  the result to the assistant, and repeat until the assistant answers with text, up to 5 rounds per message.
- **FR-019**: Invalid calculations (division by zero, square root of a negative number, missing or non-numeric
  arguments, unknown operation) and unknown actions MUST produce an error result for the assistant, never a
  failed request.
- **FR-020**: The answer MUST list the actions run for that message, each with its name, arguments and result.
- **FR-021**: Action requests and results MUST be stored in the conversation, in order, and sent back as context
  in later messages; nothing is stored when the message ends without an answer.

### Key Entities

- **Conversation**: A chat thread owned by one user. Attributes: identifier, owner, creation time, time of last
  activity.
- **Message (turn)**: One entry in a conversation, in order. Attributes: role (user, assistant or tool), kind
  (text, action request or action result), content, time.
- **Assistant action**: A named capability the assistant can ask to use, with a description and the inputs it
  expects, executed for the signed-in user. Stage 1 ships the calculator; later stages add task actions (list,
  add, search tasks).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With the feature on, 95% of valid messages receive an answer in under 10 seconds.
- **SC-002**: In a follow-up test, the assistant uses information from an earlier turn of the same conversation
  (e.g., the user's stated name) in 100% of runs.
- **SC-003**: With the switch off, 100% of chatbot requests receive "not found", and the application starts with
  no AI key configured.
- **SC-004**: 0 responses or log lines contain the AI key, verified across the error scenarios of User Story 4.
- **SC-005**: Every invalid-message and AI-failure case returns its specific error code (no generic error) in
  100% of the acceptance scenarios.
- **SC-006**: A user can never read or continue another user's conversation (0 successful cross-user accesses in
  testing).

## Assumptions

- Users are the existing signed-in users of the application; no new roles or permissions are introduced.
- The chatbot is a server feature reached by API clients; no user interface is built in this stage.
- Answers are returned whole, not streamed word by word.
- Only text is accepted; files, images and other inputs are out of scope.
- Listing, renaming or deleting conversations is out of scope for this stage.
- The default model is a current general-purpose Gemini model chosen in the plan; the operator can change it.
- Automated tests never call the real AI service; it is replaced by a stand-in during tests.
- The task features the later stages will act on do not exist yet in the application; building them is out of
  scope.
- Out of scope: assistant actions other than the calculator, the task module, task search, streaming,
  multimodal input, and a frontend.
- Calculator results are double-precision numbers; exact decimal arithmetic is not required.
