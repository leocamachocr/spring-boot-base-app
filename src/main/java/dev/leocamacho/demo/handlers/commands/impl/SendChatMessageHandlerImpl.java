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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sends the user's message, with the recent history of the conversation, to the chat provider and runs the tools
 * the assistant asks for until it answers. Provider calls run outside any transaction; the turns of the message
 * are saved together only when the assistant answers.
 */
@Component
@ConditionalOnProperty(name = "features.chatbot.enabled", havingValue = "true")
public class SendChatMessageHandlerImpl implements SendChatMessageHandler {

    private static final int MAX_MESSAGE_LENGTH = 4_000;
    private static final int MAX_TOOL_ROUNDS = 5;
    private static final String ERROR_KEY = "error";
    private static final TypeReference<List<ChatProvider.ToolCall>> TOOL_CALLS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<ChatProvider.ToolResult>> TOOL_RESULTS_TYPE = new TypeReference<>() {
    };

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatProvider chatProvider;
    private final ObjectProvider<ChatTool> tools;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;
    private final int historyMaxTurns;

    public SendChatMessageHandlerImpl(ConversationRepository conversationRepository,
                                      ChatMessageRepository messageRepository,
                                      ChatProvider chatProvider,
                                      ObjectProvider<ChatTool> tools,
                                      TransactionTemplate transactionTemplate,
                                      JsonMapper jsonMapper,
                                      @Value("${chatbot.history-max-turns}") int historyMaxTurns) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chatProvider = chatProvider;
        this.tools = tools;
        this.transactionTemplate = transactionTemplate;
        this.jsonMapper = jsonMapper;
        this.historyMaxTurns = historyMaxTurns;
    }

    @Override
    public Result handle(Command command) {
        var invalid = validate(command.message());
        if (invalid.isPresent()) {
            return invalid.get();
        }

        var conversation = getCurrentConversation(command);
        if (command.conversationId() != null && conversation.isEmpty()) {
            return new Result.ConversationNotFound();
        }

        var history = new ArrayList<ChatProvider.Turn>();
        conversation.map(this::recentTurns).ifPresent(history::addAll);

        var availableTools = toolsByName();
        var toolDefinitions = toolDefinitions(availableTools);
        var newTurns = new ArrayList<PendingTurn>();
        var invocations = new ArrayList<ToolInvocation>();
        addTurn(newTurns, new ChatProvider.Turn.Text(ChatProvider.Role.USER, command.message()));

        for (int round = 0; round <= MAX_TOOL_ROUNDS; round++) {
            var response = chatProvider.reply(new ChatProvider.Request(context(history, newTurns), toolDefinitions));
            if (!(response instanceof ChatProvider.Response.ToolCallRequested requested)) {
                return finish(response, conversation, command, newTurns, invocations);
            }
            if (round == MAX_TOOL_ROUNDS) {
                break;
            }
            var results = requested.calls().stream()
                    .map(call -> runTool(call, availableTools, command, invocations))
                    .toList();
            addTurn(newTurns, new ChatProvider.Turn.ToolCalls(requested.calls()));
            addTurn(newTurns, new ChatProvider.Turn.ToolResults(results));
        }
        return new Result.NoAnswer();
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

    /**
     * @return the user's conversation named in the command; empty when the command starts a new conversation or when
     *         the conversation does not exist or belongs to another user
     */
    private Optional<ConversationEntity> getCurrentConversation(Command command) {
        if (command.conversationId() == null) {
            return Optional.empty();
        }
        return conversationRepository.findByIdAndOwnerId(command.conversationId(), command.userId());
    }

    /**
     * The most recent turns in chronological order. A window that starts in the middle of a tool exchange is
     * trimmed to its first user text, so the provider never receives a tool result without its call.
     */
    private List<ChatProvider.Turn> recentTurns(ConversationEntity conversation) {
        var recent = new ArrayList<>(messageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(
                conversation.getId(), Limit.of(historyMaxTurns)));
        var turns = recent.reversed().stream().map(this::toTurn).toList();
        var firstUserText = 0;
        while (firstUserText < turns.size() && !isUserText(turns.get(firstUserText))) {
            firstUserText++;
        }
        return turns.subList(firstUserText, turns.size());
    }

    private static boolean isUserText(ChatProvider.Turn turn) {
        return turn instanceof ChatProvider.Turn.Text text && text.role() == ChatProvider.Role.USER;
    }

    private Map<String, ChatTool> toolsByName() {
        return tools.orderedStream().collect(Collectors.toMap(
                ChatTool::name, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
    }

    private static List<ChatProvider.ToolDefinition> toolDefinitions(Map<String, ChatTool> availableTools) {
        return availableTools.values().stream()
                .map(tool -> new ChatProvider.ToolDefinition(tool.name(), tool.description(), tool.parameters()))
                .toList();
    }

    private static List<ChatProvider.Turn> context(List<ChatProvider.Turn> history, List<PendingTurn> newTurns) {
        var context = new ArrayList<>(history);
        newTurns.forEach(pending -> context.add(pending.turn()));
        return List.copyOf(context);
    }

    private static ChatProvider.ToolResult runTool(ChatProvider.ToolCall call, Map<String, ChatTool> availableTools,
                                                   Command command, List<ToolInvocation> invocations) {
        var tool = availableTools.get(call.name());
        Map<String, Object> result;
        if (tool == null) {
            result = Map.of(ERROR_KEY, "Unknown tool: " + call.name());
        } else {
            try {
                result = tool.execute(command.userId(), call.arguments());
            } catch (RuntimeException ex) {
                result = Map.of(ERROR_KEY, "The tool failed: " + ex.getClass().getSimpleName());
            }
        }
        invocations.add(new ToolInvocation(call.name(), call.arguments(), result));
        return new ChatProvider.ToolResult(call.id(), call.name(), result);
    }

    private Result finish(ChatProvider.Response response, Optional<ConversationEntity> conversation, Command command,
                          List<PendingTurn> newTurns, List<ToolInvocation> invocations) {
        return switch (response) {
            case ChatProvider.Response.Answer answer -> {
                addTurn(newTurns, new ChatProvider.Turn.Text(ChatProvider.Role.ASSISTANT, answer.text()));
                yield saveTurns(conversation, command, newTurns, answer.text(), invocations);
            }
            case ChatProvider.Response.ToolCallRequested ignored -> new Result.NoAnswer();
            case ChatProvider.Response.Unavailable ignored -> new Result.AssistantUnavailable();
            case ChatProvider.Response.NoAnswer ignored -> new Result.NoAnswer();
        };
    }

    /**
     * Stamps each turn after the previous one, so the stored order is the order of the exchange.
     */
    private static void addTurn(List<PendingTurn> turns, ChatProvider.Turn turn) {
        var now = Instant.now();
        if (!turns.isEmpty() && !now.isAfter(turns.getLast().at())) {
            now = turns.getLast().at().plusNanos(1);
        }
        turns.add(new PendingTurn(turn, now));
    }

    private Result saveTurns(Optional<ConversationEntity> existing, Command command, List<PendingTurn> newTurns,
                             String answer, List<ToolInvocation> invocations) {
        var conversation = transactionTemplate.execute(status -> {
            var target = existing.orElseGet(() -> new ConversationEntity(command.userId(), newTurns.getFirst().at()));
            target.setLastActivityAt(newTurns.getLast().at());
            var saved = conversationRepository.save(target);
            messageRepository.saveAll(newTurns.stream().map(pending -> toEntity(saved, pending)).toList());
            return saved;
        });
        return new Result.Answered(conversation.getId(), answer, List.copyOf(invocations));
    }

    private ChatMessageEntity toEntity(ConversationEntity conversation, PendingTurn pending) {
        return switch (pending.turn()) {
            case ChatProvider.Turn.Text text -> new ChatMessageEntity(conversation,
                    text.role() == ChatProvider.Role.ASSISTANT ? ChatRole.ASSISTANT : ChatRole.USER,
                    ChatMessageKind.TEXT, text.text(), pending.at());
            case ChatProvider.Turn.ToolCalls calls -> new ChatMessageEntity(conversation, ChatRole.ASSISTANT,
                    ChatMessageKind.TOOL_CALL, jsonMapper.writeValueAsString(calls.calls()), pending.at());
            case ChatProvider.Turn.ToolResults results -> new ChatMessageEntity(conversation, ChatRole.TOOL,
                    ChatMessageKind.TOOL_RESULT, jsonMapper.writeValueAsString(results.results()), pending.at());
        };
    }

    private ChatProvider.Turn toTurn(ChatMessageEntity message) {
        return switch (message.getKind()) {
            case TEXT -> new ChatProvider.Turn.Text(
                    message.getRole() == ChatRole.ASSISTANT ? ChatProvider.Role.ASSISTANT : ChatProvider.Role.USER,
                    message.getContent());
            case TOOL_CALL -> new ChatProvider.Turn.ToolCalls(
                    jsonMapper.readValue(message.getContent(), TOOL_CALLS_TYPE));
            case TOOL_RESULT -> new ChatProvider.Turn.ToolResults(
                    jsonMapper.readValue(message.getContent(), TOOL_RESULTS_TYPE));
        };
    }

    private record PendingTurn(ChatProvider.Turn turn, Instant at) {
    }
}
