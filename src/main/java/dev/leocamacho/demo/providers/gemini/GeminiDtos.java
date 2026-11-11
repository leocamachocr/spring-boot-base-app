package dev.leocamacho.demo.providers.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.leocamacho.demo.providers.ChatProvider;

import java.util.List;
import java.util.Map;

/**
 * Wire format of the Gemini {@code generateContent} endpoint. Package-private: nothing outside this package sees it.
 */
final class GeminiDtos {

    private static final String USER_ROLE = "user";
    private static final String MODEL_ROLE = "model";

    private GeminiDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record GenerateRequest(List<Content> contents, List<Tool> tools) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Content(String role, List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Part(String text) {
    }

    record Tool(List<FunctionDeclaration> functionDeclarations) {
    }

    record FunctionDeclaration(String name, String description, Map<String, Object> parameters) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PromptFeedback(String blockReason) {
    }

    static GenerateRequest toRequest(ChatProvider.Request request) {
        var contents = request.history().stream()
                .map(turn -> new Content(toRole(turn.role()), List.of(new Part(turn.text()))))
                .toList();
        List<Tool> tools = request.tools().isEmpty()
                ? List.of()
                : List.of(new Tool(request.tools().stream()
                        .map(tool -> new FunctionDeclaration(tool.name(), tool.description(), tool.parameters()))
                        .toList()));
        return new GenerateRequest(contents, tools);
    }

    private static String toRole(ChatProvider.Role role) {
        return switch (role) {
            case USER -> USER_ROLE;
            case ASSISTANT -> MODEL_ROLE;
        };
    }
}
