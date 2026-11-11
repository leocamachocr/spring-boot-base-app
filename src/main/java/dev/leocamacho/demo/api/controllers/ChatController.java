package dev.leocamacho.demo.api.controllers;

import dev.leocamacho.demo.api.request.SendChatMessageRequest;
import dev.leocamacho.demo.api.responses.ChatAnswerResponse;
import dev.leocamacho.demo.api.responses.ErrorResponse;
import dev.leocamacho.demo.exception.ErrorCode;
import dev.leocamacho.demo.handlers.commands.SendChatMessageHandler;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/private/chat")
@ConditionalOnProperty(name = "features.chatbot.enabled", havingValue = "true")
public class ChatController {

    private final SendChatMessageHandler handler;

    public ChatController(SendChatMessageHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/messages")
    public ResponseEntity<?> sendMessage(@RequestBody SendChatMessageRequest request) {
        var session = SessionContextHolder.getSession();
        var result = handler.handle(new SendChatMessageHandler.Command(
                session.id(),
                request.conversationId(),
                request.message()
        ));
        var correlationId = session.correlationId();
        return switch (result) {
            case SendChatMessageHandler.Result.Answered answered ->
                    ResponseEntity.ok(new ChatAnswerResponse(answered.conversationId(), answered.answer()));
            case SendChatMessageHandler.Result.InvalidMessage invalid ->
                    badRequest(ErrorCode.CHAT_INVALID_MESSAGE, correlationId, invalid.reason());
            case SendChatMessageHandler.Result.ConversationNotFound ignored ->
                    badRequest(ErrorCode.CHAT_CONVERSATION_NOT_FOUND, correlationId);
            case SendChatMessageHandler.Result.AssistantUnavailable ignored ->
                    badRequest(ErrorCode.CHAT_ASSISTANT_UNAVAILABLE, correlationId);
            case SendChatMessageHandler.Result.NoAnswer ignored ->
                    badRequest(ErrorCode.CHAT_NO_ANSWER, correlationId);
        };
    }

    private static ResponseEntity<ErrorResponse> badRequest(ErrorCode code, UUID correlationId, String... params) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(code, correlationId, params));
    }
}
