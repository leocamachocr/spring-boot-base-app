# Quickstart: Verify the Migration

```bash
# Unit + ArchUnit + Checkstyle (JDK 21)
./gradlew build

# No legacy packages left (expect no output)
grep -rE "api\.(rests|types|exceptions)|\.jpa\.|\.models\." src/

# E2E against a running app
./gradlew bootRun
./gradlew e2eTest
```

Manual check of the only contract change:

```bash
curl -s -X POST localhost:8080/api/public/login -H "Content-Type: application/json" \
  -d '{"username":"nobody@mail.com","password":"wrong"}'
# → 400 {"message":"Invalid credentials","code":2003,...}
```
