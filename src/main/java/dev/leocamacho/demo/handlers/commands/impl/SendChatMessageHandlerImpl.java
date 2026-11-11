package dev.leocamacho.demo.handlers.commands.impl;

import dev.leocamacho.demo.handlers.commands.SendChatMessageHandler;
import dev.leocamacho.demo.handlers.tools.ChatTool;
import dev.leocamacho.demo.persistence.model.ChatMessageEntity;
import dev.leocamacho.demo.persistence.model.ChatMessageKind;
import dev.leocamacho.demo.persistence.model.ChatRole;
import dev.leocamacho.demo.persistence.model.ConversationEntity;
import dev.leocamacho.demo.persistence.repositories.ChatMessageRepository;
import dev.leocamacho.demo.persistence.repositories.ConversationRepository;
import dev.leocamacho.demo.providers.ChatProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Sends the user's message, with the recent history of the conversation, to the chat provider. The provider call
 * runs outside any transaction; turns are saved only when the assistant answers.
 */
@Component
@ConditionalOnProperty(name = "features.chatbot.enabled", havingValue = "true")
public class SendChatMessageHandlerImpl implements SendChatMessageHandler {

    private static final int MAX_MESSAGE_LENGTH = 4_000;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatProvider chatProvider;
    private final ObjectProvider<ChatTool> tools;
    private final TransactionTemplate transactionTemplate;
    private final int historyMaxTurns;

    public SendChatMessageHandlerImpl(ConversationRepository conversationRepository,
                                      ChatMessageRepository messageRepository,
                                      ChatProvider chatProvider,
                                      ObjectProvider<ChatTool> tools,
                                      TransactionTemplate transactionTemplate,
                                      @Value("${chatbot.history-max-turns}") int historyMaxTurns) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chatProvider = chatProvider;
        this.tools = tools;
        this.transactionTemplate = transactionTemplate;
        this.historyMaxTurns = historyMaxTurns;
    }

    @Override
    public Result handle(Command command) {
        var invalid = validate(command.message());
        if (invalid.isPresent()) {
            return invalid.get();
        }

        ConversationEntity conversation = null;
        var history = new ArrayList<ChatProvider.Turn>();
        if (command.conversationId() != null) {
            var found = conversationRepository.findByIdAndOwnerId(command.conversationId(), command.userId());
            if (found.isEmpty()) {
                return new Result.ConversationNotFound();
            }
            conversation = found.get();
            history.addAll(recentTurns(conversation));
        }

        var askedAt = Instant.now();
        history.add(new ChatProvider.Turn(ChatProvider.Role.USER, command.message()));
        var response = chatProvider.reply(new ChatProvider.Request(List.copyOf(history), toolDefinitions()));

        return switch (response) {
            case ChatProvider.Response.Answer answer -> saveTurns(conversation, command, askedAt, answer.text());
            case ChatProvider.Response.Unavailable ignored -> new Result.AssistantUnavailable();
            case ChatProvider.Response.NoAnswer ignored -> new Result.NoAnswer();
        };
    }

    private Optional<Result> validate(String message) {
        if (message == null || message.isBlank()) {
            return Optional.of(new Result.InvalidMessage("message is required"));
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return Optional.of(new Result.InvalidMessage("message is too long"));
        }
        return Optional.empty();
    }

    private List<ChatProvider.Turn> recentTurns(ConversationEntity conversation) {
        var recent = new ArrayList<>(messageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(
                conversation.getId(), Limit.of(historyMaxTurns)));
        return recent.reversed().stream()
                .map(message -> new ChatProvider.Turn(toProviderRole(message.getRole()), message.getContent()))
                .toList();
    }

    private List<ChatProvider.ToolDefinition> toolDefinitions() {
        return tools.orderedStream()
                .map(tool -> new ChatProvider.ToolDefinition(tool.name(), tool.description(), tool.parameters()))
                .toList();
    }

    private Result saveTurns(ConversationEntity existing, Command command, Instant askedAt, String answer) {
        var answeredAt = Instant.now();
        if (!answeredAt.isAfter(askedAt)) {
            answeredAt = askedAt.plusNanos(1);
        }
        var answerTime = answeredAt;
        var conversation = transactionTemplate.execute(status -> {
            var target = existing != null ? existing : new ConversationEntity(command.userId(), askedAt);
            target.setLastActivityAt(answerTime);
            var saved = conversationRepository.save(target);
            messageRepository.saveAll(List.of(
                    new ChatMessageEntity(saved, ChatRole.USER, ChatMessageKind.TEXT, command.message(), askedAt),
                    new ChatMessageEntity(saved, ChatRole.ASSISTANT, ChatMessageKind.TEXT, answer, answerTime)));
            return saved;
        });
        return new Result.Answered(conversation.getId(), answer);
    }

    private static ChatProvider.Role toProviderRole(ChatRole role) {
        return switch (role) {
            case USER -> ChatProvider.Role.USER;
            case ASSISTANT -> ChatProvider.Role.ASSISTANT;
        };
    }
}
