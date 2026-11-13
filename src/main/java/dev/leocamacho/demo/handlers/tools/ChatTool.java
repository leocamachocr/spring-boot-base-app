package dev.leocamacho.demo.handlers.tools;

import java.util.Map;
import java.util.UUID;

/**
 * An action the chatbot assistant can ask to run, such as listing or adding the user's tasks. Stage 1 ships no
 * implementations; every implementation must act only on the data of the given user.
 */
public interface ChatTool {

    String name();

    String description();

    /**
     * @return JSON schema of the arguments accepted by {@link #execute(UUID, Map)}
     */
    Map<String, Object> parameters();

    Map<String, Object> execute(UUID userId, Map<String, Object> arguments);
}
