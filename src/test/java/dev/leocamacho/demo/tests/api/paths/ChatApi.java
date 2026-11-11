package dev.leocamacho.demo.tests.api.paths;

import dev.leocamacho.demo.api.request.SendChatMessageRequest;
import dev.leocamacho.demo.api.responses.ChatAnswerResponse;

import java.util.UUID;

public class ChatApi {

    private final ApiContext context;

    public ChatApi(ApiContext context) {
        this.context = context;
    }

    public ApiResponse<ChatAnswerResponse> sendMessage(UUID conversationId, String message) {
        return ApiVerbs.doPost(
                context,
                Path.Private.Chat.MESSAGES,
                new SendChatMessageRequest(conversationId, message),
                ChatAnswerResponse.class
        );
    }
}
