package dev.leocamacho.demo.tests.api.steps;

import dev.leocamacho.demo.api.types.ErrorResponse;
import dev.leocamacho.demo.api.types.LoginResponse;
import dev.leocamacho.demo.api.types.LoginUserRequest;
import dev.leocamacho.demo.api.types.RegisterUserRequest;
import dev.leocamacho.demo.models.ErrorCode;
import dev.leocamacho.demo.tests.api.paths.ApiContext;
import dev.leocamacho.demo.tests.api.paths.UserApi;
import io.cucumber.java.en.Given;
import org.junit.jupiter.api.Assertions;

import static dev.leocamacho.demo.tests.api.steps.CucumberAdapters.getSingleRow;
import static dev.leocamacho.demo.tests.api.steps.CucumberAdapters.mapToInstance;
import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationSteps {

    private ScenarioContext scenarioContext;
    private ApiContext apiContext;
    private UserApi userApi;

    public AuthenticationSteps(ScenarioContext context, ApiContext apiContext, UserApi userApi) {
        this.scenarioContext = context;
        this.apiContext = apiContext;
        this.userApi = userApi;
    }

    @Given("I have a user with the following credentials")
    public void iHaveAUserWithTheFollowingCredentials(io.cucumber.datatable.DataTable dataTable) {
        var data = getSingleRow(dataTable);

        var request = mapToInstance(RegisterUserRequest.class, data);
        var result = userApi.registerUser(request);

        if (result.error() != null) {
            scenarioContext.set(result.error());
        } else {
            scenarioContext.set(result.response());
        }

        scenarioContext.set(request);
    }

    @Given("I am correctly registered")
    public void iAmCorrectlyRegistered() {
        var response = scenarioContext.get(RegisterUserRequest.class);
        assertNotNull(response);
        var error = scenarioContext.get(ErrorResponse.class);
        assertNull(error);

    }

    @Given("User Login with the credentials")
    public void userLoginWithTheCredentials() {
        var credentials = scenarioContext.get(RegisterUserRequest.class);

        var request = new LoginUserRequest(
                credentials.email(),
                credentials.password()
        );
        var response = userApi.loginUser(request).response();

        assertEquals(credentials.email(), response.email());

        scenarioContext.set(response);
        apiContext.token(response.token());
    }

    @Given("User Login fails with the credentials")
    public void userLoginFailsWithTheCredentials() {
        var credentials = scenarioContext.get(RegisterUserRequest.class);

        var request = new LoginUserRequest(
                credentials.email(),
                credentials.password()
        );
        var response = userApi.loginUser(request).error();
        scenarioContext.set(response);
    }

    @Given("I receive a message that the email is already registered")
    public void iReceiveAMessageThatTheEmailIsAlreadyRegistered() {
        var response = scenarioContext.get(ErrorResponse.class);
        Assertions.assertNotNull(response);
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS.code(), response.code());
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS.message(), response.message());
    }

    @Given("I have the user authenticated")
    public void iHaveTheUserAuthenticated() {
        var response = scenarioContext.get(LoginResponse.class);
        var user = userApi.loggedUser().response();

        assertEquals(response.email(), user.email());
        scenarioContext.set(user);
    }
}
