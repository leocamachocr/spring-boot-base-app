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

import java.time.Instant;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SendChatMessageHandlerTests {

    private static final int HISTORY_MAX_TURNS = 3;
    private static final int MAX_MESSAGE_LENGTH = 4_000;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID NEW_CONVERSATION_ID = UUID.randomUUID();
    private static final UUID EXISTING_CONVERSATION_ID = UUID.randomUUID();

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
                transactionTemplate, HISTORY_MAX_TURNS);
    }

    @Test
    public void newConversationIsCreatedWithBothTurnsWhenTheAssistantAnswers() {
        givenNoTools();
        givenTransactionsRun();
        givenConversationSaveAssignsId();
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.Answer("Hi there"));

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, null, "Hello"));

        assertEquals(new SendChatMessageHandler.Result.Answered(NEW_CONVERSATION_ID, "Hi there"), result);
        verify(chatProvider).reply(requestCaptor.capture());
        assertEquals(List.of(new ChatProvider.Turn(ChatProvider.Role.USER, "Hello")),
                requestCaptor.getValue().history());
        assertTrue(requestCaptor.getValue().tools().isEmpty());

        verify(messageRepository).saveAll(messagesCaptor.capture());
        var saved = messagesCaptor.getValue();
        assertEquals(2, saved.size());
        assertTurn(saved.get(0), ChatRole.USER, "Hello");
        assertTurn(saved.get(1), ChatRole.ASSISTANT, "Hi there");
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
        when(conversationRepository.findByIdAndOwnerId(EXISTING_CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(
                EXISTING_CONVERSATION_ID, Limit.of(HISTORY_MAX_TURNS)))
                .thenReturn(List.of(
                        message(conversation, ChatRole.ASSISTANT, "Nice to meet you, Ana"),
                        message(conversation, ChatRole.USER, "My name is Ana")));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.Answer("Your name is Ana"));

        var result = handler.handle(new SendChatMessageHandler.Command(
                USER_ID, EXISTING_CONVERSATION_ID, "What is my name?"));

        assertEquals(new SendChatMessageHandler.Result.Answered(EXISTING_CONVERSATION_ID, "Your name is Ana"),
                result);
        verify(chatProvider).reply(requestCaptor.capture());
        assertEquals(List.of(
                new ChatProvider.Turn(ChatProvider.Role.USER, "My name is Ana"),
                new ChatProvider.Turn(ChatProvider.Role.ASSISTANT, "Nice to meet you, Ana"),
                new ChatProvider.Turn(ChatProvider.Role.USER, "What is my name?")),
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
        when(conversationRepository.findByIdAndOwnerId(EXISTING_CONVERSATION_ID, USER_ID))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(
                EXISTING_CONVERSATION_ID, Limit.of(HISTORY_MAX_TURNS))).thenReturn(List.of());
        when(chatProvider.reply(any())).thenReturn(new ChatProvider.Response.NoAnswer(
                ChatProvider.NoAnswerReason.BLOCKED));

        var result = handler.handle(new SendChatMessageHandler.Command(USER_ID, EXISTING_CONVERSATION_ID, "Hi"));

        assertInstanceOf(SendChatMessageHandler.Result.NoAnswer.class, result);
        verify(conversationRepository, never()).save(any());
        verify(messageRepository, never()).saveAll(any());
        verifyNoInteractions(transactionTemplate);
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

    private static ConversationEntity existingConversation() {
        var conversation = new ConversationEntity(USER_ID, Instant.parse("2026-01-01T10:00:00Z"));
        conversation.setId(EXISTING_CONVERSATION_ID);
        return conversation;
    }

    private static ChatMessageEntity message(ConversationEntity conversation, ChatRole role, String content) {
        return new ChatMessageEntity(conversation, role, ChatMessageKind.TEXT, content, conversation.getCreatedAt());
    }

    private static void assertTurn(ChatMessageEntity message, ChatRole role, String content) {
        assertEquals(role, message.getRole());
        assertEquals(ChatMessageKind.TEXT, message.getKind());
        assertEquals(content, message.getContent());
    }

    private static final class FakeTool implements ChatTool {
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
            return Map.of();
        }
    }
}
