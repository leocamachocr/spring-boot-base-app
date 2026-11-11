package dev.leocamacho.demo.api.responses;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @param toolCalls actions the assistant ran for this message, in order; empty when none ran
 */
public record ChatAnswerResponse(
        UUID conversationId,
        String answer,
        List<ToolCall> toolCalls) {

    public record ToolCall(String name, Map<String, Object> arguments, Map<String, Object> result) {
    }
}
