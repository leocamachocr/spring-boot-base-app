package dev.leocamacho.demo.providers;

import java.util.List;
import java.util.Map;

/**
 * Generative chat service behind the chatbot. Implementations keep their own wire types; nothing service-specific
 * crosses this interface.
 */
public interface ChatProvider {

    Response reply(Request request);

    /**
     * @param history turns in chronological order, ending with the user's new message or with tool results
     * @param tools   actions the assistant may request
     */
    record Request(List<Turn> history, List<ToolDefinition> tools) {
    }

    sealed interface Turn {
        record Text(Role role, String text) implements Turn {
        }

        /**
         * The assistant asking to run actions.
         */
        record ToolCalls(List<ToolCall> calls) implements Turn {
        }

        /**
         * The results of the actions the assistant asked for.
         */
        record ToolResults(List<ToolResult> results) implements Turn {
        }
    }

    enum Role {
        USER,
        ASSISTANT
    }

    /**
     * @param parameters JSON schema of the tool arguments
     */
    record ToolDefinition(String name, String description, Map<String, Object> parameters) {
    }

    /**
     * @param id        identifier the service uses to match the result; may be null
     * @param signature opaque token the service requires to be sent back with this call; may be null
     */
    record ToolCall(String id, String name, Map<String, Object> arguments, String signature) {
    }

    record ToolResult(String id, String name, Map<String, Object> result) {
    }

    sealed interface Response {
        record Answer(String text) implements Response {
        }

        record ToolCallRequested(List<ToolCall> calls) implements Response {
        }

        record Unavailable(Reason reason) implements Response {
        }

        record NoAnswer(NoAnswerReason reason) implements Response {
        }
    }

    enum Reason {
        AUTH,
        QUOTA,
        TIMEOUT,
        SERVICE_ERROR
    }

    enum NoAnswerReason {
        EMPTY,
        BLOCKED
    }
}
