package dev.leocamacho.demo.handlers.commands;

import java.util.UUID;

public interface SendChatMessageHandler {

    Result handle(Command command);

    /**
     * @param conversationId {@code null} starts a new conversation
     */
    record Command(UUID userId, UUID conversationId, String message) {
    }

    sealed interface Result {
        record Answered(UUID conversationId, String answer) implements Result {
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
