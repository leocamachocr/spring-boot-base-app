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
     * @param history turns in chronological order, ending with the user's new message
     * @param tools   actions the assistant may request; always empty in stage 1
     */
    record Request(List<Turn> history, List<ToolDefinition> tools) {
    }

    record Turn(Role role, String text) {
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

    sealed interface Response {
        record Answer(String text) implements Response {
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
