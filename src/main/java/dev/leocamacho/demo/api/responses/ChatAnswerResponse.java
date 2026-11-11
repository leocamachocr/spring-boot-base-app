package dev.leocamacho.demo.api.responses;

import java.util.UUID;

public record ChatAnswerResponse(
        UUID conversationId,
        String answer) {
}
