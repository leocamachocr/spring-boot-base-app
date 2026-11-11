# Quickstart: Conversational Chatbot (Stage 1)

**Feature**: [spec.md](./spec.md)

## 1. Get a key

Create an API key in Google AI Studio. Keep it out of the repository: export it in your shell.

```bash
export GOOGLE_AI_API_KEY=<your-key>
export FEATURES_CHATBOT_ENABLED=true
```

## 2. Run the app

```bash
./gradlew bootRun
```

With `FEATURES_CHATBOT_ENABLED=true` and no key, startup fails with a message naming `GOOGLE_AI_API_KEY`.
Without the flag, the app starts as before and the chat route answers 404.

The two variables can also go in a git-ignored `.env` at the project root (see `.env.example`).

If an antivirus re-signs HTTPS traffic (e.g. Avast Web Shield), Java rejects the Gemini certificate with
`PKIX path building failed` and every message answers 3003. Run the JVM with the Windows trust store:
`-Djavax.net.ssl.trustStoreType=Windows-ROOT` (IntelliJ: *VM options*), or exclude
`generativelanguage.googleapis.com` from the antivirus HTTPS scanning.

## 3. Log in and chat

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/public/login -H 'Content-Type: application/json' \
  -d '{"username":"<email>","password":"<password>"}' | jq -r .token)

curl -s -X POST localhost:8080/api/private/chat/messages \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"message":"Hi, my name is Ana"}'
# -> {"conversationId":"<id>","answer":"..."}

curl -s -X POST localhost:8080/api/private/chat/messages \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"conversationId":"<id>","message":"What is my name?"}'
# -> the answer mentions Ana
```

## 4. Tests

```bash
./gradlew build                                   # unit + ArchUnit + Checkstyle; never calls Google
./gradlew e2eTest                                 # flag off: runs @chatbot-disabled (404)
./gradlew e2eTest "-Dcucumber.filter.tags=@chatbot-live"   # app running with flag on and a real key
```
