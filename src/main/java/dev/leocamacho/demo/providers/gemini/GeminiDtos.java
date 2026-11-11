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
    record Part(String text, FunctionCall functionCall, FunctionResponse functionResponse, String thoughtSignature) {

        static Part ofText(String text) {
            return new Part(text, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FunctionCall(String id, String name, Map<String, Object> args) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FunctionResponse(String id, String name, Map<String, Object> response) {
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
                .map(GeminiDtos::toContent)
                .toList();
        List<Tool> tools = request.tools().isEmpty()
                ? List.of()
                : List.of(new Tool(request.tools().stream()
                        .map(tool -> new FunctionDeclaration(tool.name(), tool.description(), tool.parameters()))
                        .toList()));
        return new GenerateRequest(contents, tools);
    }

    /**
     * @return the tool calls in the parts, empty when the parts carry no function call
     */
    static List<ChatProvider.ToolCall> toolCalls(List<Part> parts) {
        return parts.stream()
                .filter(part -> part.functionCall() != null)
                .map(part -> new ChatProvider.ToolCall(
                        part.functionCall().id(),
                        part.functionCall().name(),
                        part.functionCall().args() == null ? Map.of() : part.functionCall().args(),
                        part.thoughtSignature()))
                .toList();
    }

    private static Content toContent(ChatProvider.Turn turn) {
        return switch (turn) {
            case ChatProvider.Turn.Text text -> new Content(toRole(text.role()), List.of(Part.ofText(text.text())));
            case ChatProvider.Turn.ToolCalls calls -> new Content(MODEL_ROLE, calls.calls().stream()
                    .map(call -> new Part(null, new FunctionCall(call.id(), call.name(), call.arguments()), null,
                            call.signature()))
                    .toList());
            case ChatProvider.Turn.ToolResults results -> new Content(USER_ROLE, results.results().stream()
                    .map(result -> new Part(null, null,
                            new FunctionResponse(result.id(), result.name(), result.result()), null))
                    .toList());
        };
    }

    private static String toRole(ChatProvider.Role role) {
        return switch (role) {
            case USER -> USER_ROLE;
            case ASSISTANT -> MODEL_ROLE;
        };
    }
}
