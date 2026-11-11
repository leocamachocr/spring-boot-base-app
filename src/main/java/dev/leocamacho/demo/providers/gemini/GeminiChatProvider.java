package dev.leocamacho.demo.providers.gemini;

import dev.leocamacho.demo.providers.ChatProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "features.chatbot.enabled", havingValue = "true")
public class GeminiChatProvider implements ChatProvider {

    private static final String MISSING_KEY_MESSAGE =
            "google.ai.api-key (GOOGLE_AI_API_KEY) must be set when features.chatbot.enabled=true";
    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiChatProvider.class);
    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final String GENERATE_PATH = "/models/{model}:generateContent";
    private static final String INVALID_KEY_STATUS = "API_KEY_INVALID";
    private static final Set<String> BLOCKED_FINISH_REASONS =
            Set.of("SAFETY", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII", "RECITATION");

    private final RestClient restClient;
    private final String model;

    public GeminiChatProvider(@Value("${google.ai.api-key:}") String apiKey,
                              @Value("${google.ai.model}") String model,
                              @Value("${google.ai.base-url}") String baseUrl,
                              @Value("${google.ai.timeout}") Duration timeout) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(MISSING_KEY_MESSAGE);
        }
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .requestFactory(requestFactory)
                .build();
        this.model = model;
    }

    @Override
    public Response reply(Request request) {
        try {
            var body = restClient.post()
                    .uri(GENERATE_PATH, model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GeminiDtos.toRequest(request))
                    .retrieve()
                    .body(GeminiDtos.GenerateResponse.class);
            return toResponse(body);
        } catch (RestClientResponseException ex) {
            var reason = reasonFor(ex);
            LOGGER.warn("Gemini call failed: model={}, status={}, reason={}", model, ex.getStatusCode().value(),
                    reason);
            return new Response.Unavailable(reason);
        } catch (RestClientException ex) {
            var reason = causedByTimeout(ex) ? Reason.TIMEOUT : Reason.SERVICE_ERROR;
            LOGGER.warn("Gemini call failed: model={}, reason={}, error={}", model, reason, rootCauseName(ex));
            return new Response.Unavailable(reason);
        }
    }

    /**
     * Only real timeouts count: a read timeout can surface wrapped in a generic {@link RestClientException}, while
     * TLS, DNS or connection failures also arrive as {@code ResourceAccessException} and are not timeouts.
     */
    private static boolean causedByTimeout(Throwable ex) {
        for (var cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof InterruptedIOException) {
                return true;
            }
        }
        return false;
    }

    private static String rootCauseName(Throwable ex) {
        var root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName();
    }

    private Response toResponse(GeminiDtos.GenerateResponse body) {
        if (body == null) {
            return noAnswer(NoAnswerReason.EMPTY);
        }
        if (body.promptFeedback() != null && body.promptFeedback().blockReason() != null) {
            return noAnswer(NoAnswerReason.BLOCKED);
        }
        if (body.candidates() == null || body.candidates().isEmpty()) {
            return noAnswer(NoAnswerReason.EMPTY);
        }
        var candidate = body.candidates().getFirst();
        if (BLOCKED_FINISH_REASONS.contains(candidate.finishReason())) {
            return noAnswer(NoAnswerReason.BLOCKED);
        }
        var parts = candidate.content() == null || candidate.content().parts() == null
                ? List.<GeminiDtos.Part>of()
                : candidate.content().parts();
        var toolCalls = GeminiDtos.toolCalls(parts);
        if (!toolCalls.isEmpty()) {
            return new Response.ToolCallRequested(toolCalls);
        }
        var text = parts.stream()
                .map(GeminiDtos.Part::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
        if (text.isBlank()) {
            return noAnswer(NoAnswerReason.EMPTY);
        }
        return new Response.Answer(text);
    }

    private Response noAnswer(NoAnswerReason reason) {
        LOGGER.warn("Gemini returned no answer: model={}, reason={}", model, reason);
        return new Response.NoAnswer(reason);
    }

    private static Reason reasonFor(RestClientResponseException ex) {
        var status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            return Reason.AUTH;
        }
        if (status == HttpStatus.BAD_REQUEST && ex.getResponseBodyAsString().contains(INVALID_KEY_STATUS)) {
            return Reason.AUTH;
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            return Reason.QUOTA;
        }
        return Reason.SERVICE_ERROR;
    }
}
