package dev.leocamacho.demo.handlers.commands;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SendChatMessageHandler {

    Result handle(Command command);

    /**
     * @param conversationId {@code null} starts a new conversation
     */
    record Command(UUID userId, UUID conversationId, String message) {
    }

    /**
     * An action the assistant ran while answering the message.
     */
    record ToolInvocation(String name, Map<String, Object> arguments, Map<String, Object> result) {
    }

    sealed interface Result {
        /**
         * @param toolCalls actions run for this message, in order; empty when none ran
         */
        record Answered(UUID conversationId, String answer, List<ToolInvocation> toolCalls) implements Result {
        }

        record InvalidMessage(String reason) implements Result {
        }

        record ConversationNotFound() implements Result {
        }

        record AssistantUnavailable() implements Result {
        }

        record NoAnswer() implements Result {
        }
    }
}
