package dev.leocamacho.demo.tests.providers;

import com.sun.net.httpserver.HttpServer;
import dev.leocamacho.demo.providers.ChatProvider;
import dev.leocamacho.demo.providers.gemini.GeminiChatProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the provider against a local HTTP server that imitates the Gemini endpoint; Google is never called.
 */
public class GeminiChatProviderTests {

    private static final String API_KEY = "test-key-7f3a";
    private static final String MODEL = "test-model";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(200);
    private static final long SLOW_RESPONSE_MILLIS = 1_000;
    private static final String TEXT_ANSWER = """
            {"candidates":[{"content":{"role":"model","parts":[{"text":"Hello "},{"text":"Ana"}]},
             "finishReason":"STOP"}]}""";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private HttpServer server;
    private String baseUrl;
    private volatile int responseStatus;
    private volatile String responseBody;
    private volatile long responseDelayMillis;
    private volatile String receivedPath;
    private volatile String receivedQuery;
    private volatile String receivedKey;
    private volatile String receivedBody;

    @BeforeEach
    public void startServer() throws IOException {
        responseStatus = HttpStatus.OK.value();
        responseBody = TEXT_ANSWER;
        responseDelayMillis = 0;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            receivedPath = exchange.getRequestURI().getPath();
            receivedQuery = exchange.getRequestURI().getRawQuery();
            receivedKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            pause(responseDelayMillis);
            var bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta";
    }

    @AfterEach
    public void stopServer() {
        server.stop(0);
    }

    @Test
    public void requestCarriesTheKeyInAHeaderAndTheTurnsWithGeminiRoles() {
        var provider = provider(TIMEOUT);

        provider.reply(new ChatProvider.Request(List.of(
                new ChatProvider.Turn(ChatProvider.Role.USER, "My name is Ana"),
                new ChatProvider.Turn(ChatProvider.Role.ASSISTANT, "Nice to meet you"),
                new ChatProvider.Turn(ChatProvider.Role.USER, "What is my name?")), List.of()));

        assertEquals("/v1beta/models/" + MODEL + ":generateContent", receivedPath);
        assertEquals(API_KEY, receivedKey);
        assertTrue(receivedQuery == null || !receivedQuery.contains(API_KEY));
        var body = JSON.readTree(receivedBody);
        assertFalse(body.has("tools"));
        var contents = body.get("contents");
        assertEquals(List.of("user", "model", "user"),
                contents.valueStream().map(content -> content.get("role").asString()).toList());
        assertEquals("What is my name?", contents.get(2).get("parts").get(0).get("text").asString());
    }

    @Test
    public void toolsAreSentAsFunctionDeclarations() {
        var provider = provider(TIMEOUT);

        provider.reply(new ChatProvider.Request(
                List.of(new ChatProvider.Turn(ChatProvider.Role.USER, "List my tasks")),
                List.of(new ChatProvider.ToolDefinition("listTasks", "Lists the user's tasks",
                        Map.of("type", "object")))));

        JsonNode declaration = JSON.readTree(receivedBody).get("tools").get(0).get("functionDeclarations").get(0);
        assertEquals("listTasks", declaration.get("name").asString());
        assertEquals("Lists the user's tasks", declaration.get("description").asString());
        assertEquals("object", declaration.get("parameters").get("type").asString());
    }

    @Test
    public void textPartsAreJoinedIntoTheAnswer() {
        var response = provider(TIMEOUT).reply(singleTurn());

        assertEquals(new ChatProvider.Response.Answer("Hello Ana"), response);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    public void missingKeyFailsWithAMessageThatNamesTheSetting(String apiKey) {
        var error = assertThrows(IllegalStateException.class,
                () -> new GeminiChatProvider(apiKey, MODEL, baseUrl, TIMEOUT));

        assertTrue(error.getMessage().contains("google.ai.api-key"));
        assertTrue(error.getMessage().contains("GOOGLE_AI_API_KEY"));
    }

    @ParameterizedTest
    @MethodSource("httpErrors")
    public void httpErrorsMakeTheAssistantUnavailable(HttpStatus status, String body, ChatProvider.Reason reason) {
        responseStatus = status.value();
        responseBody = body;

        var response = provider(TIMEOUT).reply(singleTurn());

        assertEquals(new ChatProvider.Response.Unavailable(reason), response);
    }

    @Test
    public void slowServiceTimesOut() {
        responseDelayMillis = SLOW_RESPONSE_MILLIS;

        var response = provider(SHORT_TIMEOUT).reply(singleTurn());

        assertEquals(new ChatProvider.Response.Unavailable(ChatProvider.Reason.TIMEOUT), response);
    }

    @Test
    public void tlsFailureIsAServiceErrorNotATimeout() throws IOException, InterruptedException {
        try (var plainSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            var answerInPlainText = Thread.ofVirtual().start(() -> {
                try (Socket client = plainSocket.accept()) {
                    client.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                    // The provider side of the exchange is what the test asserts on.
                }
            });
            var httpsUrl = "https://127.0.0.1:" + plainSocket.getLocalPort() + "/v1beta";

            var response = new GeminiChatProvider(API_KEY, MODEL, httpsUrl, TIMEOUT).reply(singleTurn());

            answerInPlainText.join();
            assertEquals(new ChatProvider.Response.Unavailable(ChatProvider.Reason.SERVICE_ERROR), response);
        }
    }

    @ParameterizedTest
    @MethodSource("missingAnswers")
    public void missingOrBlockedAnswersAreReportedAsNoAnswer(String body, ChatProvider.NoAnswerReason reason) {
        responseBody = body;

        var response = provider(TIMEOUT).reply(singleTurn());

        assertEquals(new ChatProvider.Response.NoAnswer(reason), response);
    }

    private static Stream<Arguments> httpErrors() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, "{\"error\":{\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}",
                        ChatProvider.Reason.AUTH),
                Arguments.of(HttpStatus.BAD_REQUEST, "{\"error\":{\"status\":\"INVALID_ARGUMENT\"}}",
                        ChatProvider.Reason.SERVICE_ERROR),
                Arguments.of(HttpStatus.UNAUTHORIZED, "{}", ChatProvider.Reason.AUTH),
                Arguments.of(HttpStatus.FORBIDDEN, "{}", ChatProvider.Reason.AUTH),
                Arguments.of(HttpStatus.TOO_MANY_REQUESTS, "{}", ChatProvider.Reason.QUOTA),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, "{}", ChatProvider.Reason.SERVICE_ERROR),
                Arguments.of(HttpStatus.SERVICE_UNAVAILABLE, "{}", ChatProvider.Reason.SERVICE_ERROR));
    }

    private static Stream<Arguments> missingAnswers() {
        return Stream.of(
                Arguments.of("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", ChatProvider.NoAnswerReason.BLOCKED),
                Arguments.of("{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}", ChatProvider.NoAnswerReason.BLOCKED),
                Arguments.of("{\"candidates\":[]}", ChatProvider.NoAnswerReason.EMPTY),
                Arguments.of("{}", ChatProvider.NoAnswerReason.EMPTY),
                Arguments.of(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\" \"}]},\"finishReason\":\"STOP\"}]}",
                        ChatProvider.NoAnswerReason.EMPTY));
    }

    private GeminiChatProvider provider(Duration timeout) {
        return new GeminiChatProvider(API_KEY, MODEL, baseUrl, timeout);
    }

    private static ChatProvider.Request singleTurn() {
        return new ChatProvider.Request(List.of(new ChatProvider.Turn(ChatProvider.Role.USER, "Hi")), List.of());
    }

    private static void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
