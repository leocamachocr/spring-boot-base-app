package dev.leocamacho.demo.tests.handlers.commands;

import dev.leocamacho.demo.handlers.commands.SendChatMessageHandler;
import dev.leocamacho.demo.handlers.commands.impl.SendChatMessageHandlerImpl;
import dev.leocamacho.demo.handlers.tools.ChatTool;
import dev.leocamacho.demo.persistence.model.ChatMessageEntity;
import dev.leocamacho.demo.persistence.model.ChatMessageKind;
import dev.leocamacho.demo.persistence.model.ChatRole;
import dev.leocamacho.demo.persistence.model.ConversationEntity;
import dev.leocamacho.demo.persistence.repositories.ChatMessageRepository;
import dev.leocamacho.demo.persistence.repositories.ConversationRepository;
import dev.leocamacho.demo.providers.ChatProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Limit;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SendChatMessageHandlerTests {

    private static final int HISTORY_MAX_TURNS = 3;
    private static final int MAX_MESSAGE_LENGTH = 4_000;
    private static final int MAX_TOOL_ROUNDS = 5;
    private static final int DOUBLED = 4;
    private static final int MESSAGES_WITH_ONE_TOOL_ROUND = 4;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID NEW_CONVERSATION_ID = UUID.randomUUID();
    private static final UUID EXISTING_CONVERSATION_ID = UUID.randomUUID();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final ChatProvider.ToolCall FAKE_CALL =
            new ChatProvider.ToolCall("call-1", "fakeTool", Map.of("x", 2), "sig-1");

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private ChatProvider chatProvider;
    @Mock
    private ObjectProvider<ChatTool> tools;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Captor
    private ArgumentCaptor<ChatProvider.Request> requestCaptor;
    @Captor
    private ArgumentCaptor<List<ChatMessageEntity>> messagesCaptor;

    private SendChatMessageHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new SendChatMessageHandlerImpl(conversationRepository, messageRepository, chatProvider, tools,
                transactionTemplate, JSON, HISTORY_MAX_TURNS);
    }

    @Test
    public void newConversationIsCreatedWithBothTurnsWhenTheAssistantAnswers() {
        givenNoTools();
        givenTransactionsRun();
        givenConversationSaveAssignsId();
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.Answer("Hi there"));

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, null, "Hello"));

        assertEquals(new SendChatMessageHandler.Result.Answered(NEW_CONVERSATION_ID, "Hi there", List.of()), result);
        verify(chatProvider).reply(requestCaptor.capture());
        assertEquals(List.of(userText("Hello")), requestCaptor.getValue().history());
        assertTrue(requestCaptor.getValue().tools().isEmpty());

        verify(messageRepository).saveAll(messagesCaptor.capture());
        var saved = messagesCaptor.getValue();
        assertEquals(2, saved.size());
        assertMessage(saved.get(0), ChatRole.USER, ChatMessageKind.TEXT);
        assertEquals("Hello", saved.get(0).getContent());
        assertMessage(saved.get(1), ChatRole.ASSISTANT, ChatMessageKind.TEXT);
        assertEquals("Hi there", saved.get(1).getContent());
        assertEquals(USER_ID, saved.get(0).getConversation().getOwnerId());
        assertTrue(saved.get(1).getCreatedAt().isAfter(saved.get(0).getCreatedAt()));
    }

    @Test
    public void registeredToolsAreOfferedToTheProvider() {
        when(tools.orderedStream()).thenReturn(Stream.of(new FakeTool()));
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.Unavailable(
                ChatProvider.Reason.SERVICE_ERROR));

        handler.handle(new SendChatMessageHandler.Command(USER_ID, null, "Hello"));

        verify(chatProvider).reply(requestCaptor.capture());
        assertEquals(List.of(new ChatProvider.ToolDefinition("fakeTool", "A fake tool", Map.of("type", "object"))),
                requestCaptor.getValue().tools());
    }

    @Test
    public void existingConversationSendsRecentHistoryInChronologicalOrder() {
        var conversation = existingConversation();
        givenNoTools();
        givenTransactionsRun();
        givenExistingConversation(conversation, List.of(
                message(conversation, ChatRole.ASSISTANT, ChatMessageKind.TEXT, "Nice to meet you, Ana"),
                message(conversation, ChatRole.USER, ChatMessageKind.TEXT, "My name is Ana")));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.Answer("Your name is Ana"));

        var result = handler.handle(new SendChatMessageHandler.Command(
                USER_ID, EXISTING_CONVERSATION_ID, "What is my name?"));

        assertEquals(new SendChatMessageHandler.Result.Answered(
                EXISTING_CONVERSATION_ID, "Your name is Ana", List.of()), result);
        verify(chatProvider).reply(requestCaptor.capture());
        assertEquals(List.of(
                userText("My name is Ana"),
                new ChatProvider.Turn.Text(ChatProvider.Role.ASSISTANT, "Nice to meet you, Ana"),
                userText("What is my name?")),
                requestCaptor.getValue().history());

        verify(messageRepository).saveAll(messagesCaptor.capture());
        assertTrue(messagesCaptor.getValue().stream()
                .allMatch(saved -> saved.getConversation() == conversation));
        assertTrue(conversation.getLastActivityAt().isAfter(conversation.getCreatedAt()));
    }

    @Test
    public void unknownOrForeignConversationIsNotFound() {
        when(conversationRepository.findByIdAndOwnerId(EXISTING_CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, EXISTING_CONVERSATION_ID, "Hi"));

        assertInstanceOf(SendChatMessageHandler.Result.ConversationNotFound.class, result);
        verify(conversationRepository, never()).save(any());
        verifyNoInteractions(chatProvider, messageRepository, transactionTemplate);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void missingMessageIsInvalid(String message) {
        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, null, message));

        assertInstanceOf(SendChatMessageHandler.Result.InvalidMessage.class, result);
        verifyNoInteractions(conversationRepository, messageRepository, chatProvider, transactionTemplate);
    }

    @Test
    public void messageOverTheLimitIsInvalid() {
        var result = handler.handle(new SendChatMessageHandler.Command(
                USER_ID, null, "a".repeat(MAX_MESSAGE_LENGTH + 1)));

        assertInstanceOf(SendChatMessageHandler.Result.InvalidMessage.class, result);
        verifyNoInteractions(conversationRepository, messageRepository, chatProvider, transactionTemplate);
    }

    @Test
    public void messageAtTheLimitIsAccepted() {
        givenNoTools();
        givenTransactionsRun();
        givenConversationSaveAssignsId();
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.Answer("ok"));

        var result = handler.handle(new SendChatMessageHandler.Command(
                USER_ID, null, "a".repeat(MAX_MESSAGE_LENGTH)));

        assertInstanceOf(SendChatMessageHandler.Result.Answered.class, result);
    }

    @Test
    public void unavailableAssistantWritesNothing() {
        givenNoTools();
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.Unavailable(ChatProvider.Reason.QUOTA));

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, null, "Hello"));

        assertInstanceOf(SendChatMessageHandler.Result.AssistantUnavailable.class, result);
        verify(conversationRepository, never()).save(any());
        verifyNoInteractions(messageRepository, transactionTemplate);
    }

    @Test
    public void missingAnswerInExistingConversationWritesNothing() {
        var conversation = existingConversation();
        givenNoTools();
        givenExistingConversation(conversation, List.of());
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.NoAnswer(
                ChatProvider.NoAnswerReason.BLOCKED));

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, EXISTING_CONVERSATION_ID, "Hi"));

        assertInstanceOf(SendChatMessageHandler.Result.NoAnswer.class, result);
        verify(conversationRepository, never()).save(any());
        verify(messageRepository, never()).saveAll(any());
        verifyNoInteractions(transactionTemplate);
    }

    @Test
    public void requestedToolRunsAndItsResultGoesBackToTheAssistant() {
        var tool = new FakeTool();
        when(tools.orderedStream()).thenReturn(Stream.of(tool));
        givenTransactionsRun();
        givenConversationSaveAssignsId();
        when(chatProvider.reply(any()))
                .thenReturn(new ChatProvider.Response.ToolCallRequested(List.of(FAKE_CALL)))
                .thenReturn(new ChatProvider.Response.Answer("The answer is 4"));

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, null, "Double 2"));

        var expectedResult = Map.<String, Object>of("result", DOUBLED);
        assertEquals(new SendChatMessageHandler.Result.Answered(NEW_CONVERSATION_ID, "The answer is 4",
                List.of(new SendChatMessageHandler.ToolInvocation("fakeTool", Map.of("x", 2), expectedResult))),
                result);
        assertEquals(List.of(USER_ID), tool.calledBy);

        verify(chatProvider, times(2)).reply(requestCaptor.capture());
        assertEquals(List.of(
                userText("Double 2"),
                new ChatProvider.Turn.ToolCalls(List.of(FAKE_CALL)),
                new ChatProvider.Turn.ToolResults(List.of(
                        new ChatProvider.ToolResult("call-1", "fakeTool", expectedResult)))),
                requestCaptor.getAllValues().get(1).history());

        verify(messageRepository).saveAll(messagesCaptor.capture());
        var saved = messagesCaptor.getValue();
        assertEquals(MESSAGES_WITH_ONE_TOOL_ROUND, saved.size());
        assertMessage(saved.get(0), ChatRole.USER, ChatMessageKind.TEXT);
        assertMessage(saved.get(1), ChatRole.ASSISTANT, ChatMessageKind.TOOL_CALL);
        assertMessage(saved.get(2), ChatRole.TOOL, ChatMessageKind.TOOL_RESULT);
        assertMessage(saved.getLast(), ChatRole.ASSISTANT, ChatMessageKind.TEXT);
        assertTrue(saved.get(1).getContent().contains("sig-1"));
        for (int i = 1; i < saved.size(); i++) {
            assertTrue(saved.get(i).getCreatedAt().isAfter(saved.get(i - 1).getCreatedAt()));
        }
    }

    @Test
    public void unknownToolProducesAnErrorResultForTheAssistant() {
        givenNoTools();
        givenTransactionsRun();
        givenConversationSaveAssignsId();
        when(chatProvider.reply(any()))
                .thenReturn(new ChatProvider.Response.ToolCallRequested(List.of(FAKE_CALL)))
                .thenReturn(new ChatProvider.Response.Answer("I could not do that"));

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, null, "Double 2"));

        var answered = assertInstanceOf(SendChatMessageHandler.Result.Answered.class, result);
        assertEquals(Map.of("error", "Unknown tool: fakeTool"), answered.toolCalls().getFirst().result());
    }

    @Test
    public void assistantThatNeverStopsCallingToolsGetsNoAnswerAndNothingIsSaved() {
        when(tools.orderedStream()).thenReturn(Stream.of(new FakeTool()));
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.ToolCallRequested(List.of(FAKE_CALL)));

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, null, "Loop"));

        assertInstanceOf(SendChatMessageHandler.Result.NoAnswer.class, result);
        verify(chatProvider, times(MAX_TOOL_ROUNDS + 1)).reply(any());
        verifyNoInteractions(messageRepository, transactionTemplate);
    }

    @Test
    public void storedToolTurnsAreReplayedAndOrphanResultsAreTrimmed() {
        var conversation = existingConversation();
        var call = JSON.writeValueAsString(List.of(FAKE_CALL));
        var results = JSON.writeValueAsString(List.of(
                new ChatProvider.ToolResult("call-1", "fakeTool", Map.of("result", DOUBLED))));
        givenNoTools();
        givenTransactionsRun();
        givenExistingConversation(conversation, new ArrayList<>(List.of(
                message(conversation, ChatRole.ASSISTANT, ChatMessageKind.TEXT, "It is 4"),
                message(conversation, ChatRole.TOOL, ChatMessageKind.TOOL_RESULT, results),
                message(conversation, ChatRole.ASSISTANT, ChatMessageKind.TOOL_CALL, call),
                message(conversation, ChatRole.USER, ChatMessageKind.TEXT, "Double 2"),
                message(conversation, ChatRole.TOOL, ChatMessageKind.TOOL_RESULT, results))));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.Answer("You asked to double 2"));

        handler.handle(new SendChatMessageHandler.Command(USER_ID, EXISTING_CONVERSATION_ID, "What did I ask?"));

        verify(chatProvider).reply(requestCaptor.capture());
        var history = requestCaptor.getValue().history();
        assertEquals(List.of(
                userText("Double 2"),
                new ChatProvider.Turn.ToolCalls(List.of(FAKE_CALL)),
                new ChatProvider.Turn.ToolResults(List.of(
                        new ChatProvider.ToolResult("call-1", "fakeTool", Map.of("result", DOUBLED)))),
                new ChatProvider.Turn.Text(ChatProvider.Role.ASSISTANT, "It is 4"),
                userText("What did I ask?")),
                history);
    }

    private void givenNoTools() {
        when(tools.orderedStream()).thenReturn(Stream.empty());
    }

    private void givenTransactionsRun() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private void givenConversationSaveAssignsId() {
        when(conversationRepository.save(any())).thenAnswer(invocation -> {
            ConversationEntity conversation = invocation.getArgument(0);
            conversation.setId(NEW_CONVERSATION_ID);
            return conversation;
        });
    }

    private void givenExistingConversation(ConversationEntity conversation, List<ChatMessageEntity> newestFirst) {
        when(conversationRepository.findByIdAndOwnerId(EXISTING_CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(
                EXISTING_CONVERSATION_ID, Limit.of(HISTORY_MAX_TURNS))).thenReturn(newestFirst);
    }

    private static ConversationEntity existingConversation() {
        var conversation = new ConversationEntity(USER_ID, Instant.parse("2026-01-01T10:00:00Z"));
        conversation.setId(EXISTING_CONVERSATION_ID);
        return conversation;
    }

    private static ChatMessageEntity message(ConversationEntity conversation, ChatRole role, ChatMessageKind kind,
                                             String content) {
        return new ChatMessageEntity(conversation, role, kind, content, conversation.getCreatedAt());
    }

    private static ChatProvider.Turn userText(String text) {
        return new ChatProvider.Turn.Text(ChatProvider.Role.USER, text);
    }

    private static void assertMessage(ChatMessageEntity message, ChatRole role, ChatMessageKind kind) {
        assertEquals(role, message.getRole());
        assertEquals(kind, message.getKind());
    }

    private static final class FakeTool implements ChatTool {
        private final List<UUID> calledBy = new ArrayList<>();

        @Override
        public String name() {
            return "fakeTool";
        }

        @Override
        public String description() {
            return "A fake tool";
        }

        @Override
        public Map<String, Object> parameters() {
            return Map.of("type", "object");
        }

        @Override
        public Map<String, Object> execute(UUID userId, Map<String, Object> arguments) {
            calledBy.add(userId);
            return Map.of("result", ((Integer) arguments.get("x")) * 2);
        }
    }
}
