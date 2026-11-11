package dev.leocamacho.demo.api.request;

import java.util.UUID;

public record SendChatMessageRequest(
        UUID conversationId,
        String message) {
}
