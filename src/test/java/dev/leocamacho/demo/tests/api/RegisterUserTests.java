package dev.leocamacho.demo.tests.api;

import dev.leocamacho.demo.api.types.RegisterUserRequest;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static dev.leocamacho.RestAssuredDefaults.givenThis;
import static dev.leocamacho.demo.tests.fakers.api.Builders.buildRegisterUserRequestWithValidParams;
import static dev.leocamacho.demo.tests.validators.UUIDValidator.isValidUUID;
import static org.hamcrest.MatcherAssert.assertThat;

public class RegisterUserTests {
    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    //using rest-assured to test the api
    @Test
    public void testRegisterUser() {
        // Given
        RegisterUserRequest requestBody = buildRegisterUserRequestWithValidParams("Alice");

        // When
        var result = givenThis(requestBody)
                .post("/api/public/register")
                .then()
                .statusCode(200)
                .extract().path("result").toString();

        // Then
        assertThat(isValidUUID(result), Matchers.is(true));

    }
}
