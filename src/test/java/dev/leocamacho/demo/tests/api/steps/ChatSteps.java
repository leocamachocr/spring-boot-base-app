package dev.leocamacho.demo.tests.api.steps;

import dev.leocamacho.demo.api.request.LoginUserRequest;
import dev.leocamacho.demo.api.request.RegisterUserRequest;
import dev.leocamacho.demo.api.responses.ChatAnswerResponse;
import dev.leocamacho.demo.tests.api.paths.ApiContext;
import dev.leocamacho.demo.tests.api.paths.ApiResponse;
import dev.leocamacho.demo.tests.api.paths.ChatApi;
import dev.leocamacho.demo.tests.api.paths.UserApi;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatSteps {

    private final ApiContext apiContext;
    private final UserApi userApi;
    private final ChatApi chatApi;
    private ApiResponse<ChatAnswerResponse> lastResponse;
    private UUID conversationId;

    public ChatSteps(ApiContext apiContext, UserApi userApi, ChatApi chatApi) {
        this.apiContext = apiContext;
        this.userApi = userApi;
        this.chatApi = chatApi;
    }

    @When("User sends the chat message {string}")
    public void userSendsTheChatMessage(String message) {
        send(null, message);
    }

    @When("User sends the chat message {string} in the same conversation")
    public void userSendsTheChatMessageInTheSameConversation(String message) {
        assertNotNull(conversationId, "No conversation was started in this scenario");
        send(conversationId, message);
    }

    @When("User sends the chat message {string} in an unknown conversation")
    public void userSendsTheChatMessageInAnUnknownConversation(String message) {
        send(UUID.randomUUID(), message);
    }

    @When("User sends a blank chat message")
    public void userSendsABlankChatMessage() {
        send(null, "   ");
    }

    @When("User sends a chat message of {int} characters")
    public void userSendsAChatMessageOfCharacters(int length) {
        send(null, "a".repeat(length));
    }

    @Given("Another user is registered and logged in")
    public void anotherUserIsRegisteredAndLoggedIn() {
        var email = UUID.randomUUID() + "@email.com";
        var password = "password";
        userApi.registerUser(new RegisterUserRequest("Other", email, password)).response();
        var login = userApi.loginUser(new LoginUserRequest(email, password)).response();
        apiContext.token(login.token());
    }

    @Then("User receives an answer in a new conversation")
    public void userReceivesAnAnswerInANewConversation() {
        assertNull(lastResponse.error(), () -> "Unexpected error: " + lastResponse.error());
        var answer = lastResponse.response();
        assertNotNull(answer.conversationId());
        assertFalse(answer.answer().isBlank());
    }

    @Then("the answer mentions {string}")
    public void theAnswerMentions(String expected) {
        assertNull(lastResponse.error(), () -> "Unexpected error: " + lastResponse.error());
        var answer = lastResponse.response().answer().toLowerCase(Locale.ROOT);
        assertTrue(answer.contains(expected.toLowerCase(Locale.ROOT)),
                () -> "Answer does not mention " + expected + ": " + answer);
    }

    @Then("the assistant used the tool {string} with operation {string}")
    public void theAssistantUsedTheToolWithOperation(String tool, String operation) {
        assertNull(lastResponse.error(), () -> "Unexpected error: " + lastResponse.error());
        var toolCalls = lastResponse.response().toolCalls();
        assertTrue(toolCalls.stream().anyMatch(call -> tool.equals(call.name())
                        && operation.equals(String.valueOf(call.arguments().get("operation")))),
                () -> "Expected a " + tool + " call with operation " + operation + ", got " + toolCalls);
    }

    @Then("User receives a chat error with code {int}")
    public void userReceivesAChatErrorWithCode(int code) {
        assertNotNull(lastResponse.error(), "Expected an error response");
        assertEquals(code, lastResponse.error().code());
    }

    private void send(UUID targetConversation, String message) {
        lastResponse = chatApi.sendMessage(targetConversation, message);
        if (lastResponse.error() == null && conversationId == null) {
            conversationId = lastResponse.response().conversationId();
        }
    }
}
