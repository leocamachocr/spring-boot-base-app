package dev.leocamacho.demo.tests.api.paths;

import dev.leocamacho.demo.api.types.ErrorResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


public class ApiVerbs {
    private static final String DEFAULT_HOST = System.getProperty("host", "http://localhost");
    private static final int DEFAULT_PORT = Integer.parseInt(System.getProperty("port", "8080"));

    static {
        setupTargetHost(DEFAULT_HOST, DEFAULT_PORT);
    }

    public static void setupTargetHost(String host, int port) {
        RestAssured.baseURI = host;
        RestAssured.port = port;
    }

    public static <I, R> ApiResponse<R> doPost(ApiContext context, Path path, I input, Class<R> responseType) {
        var response = givenThis(input)
                .header("Authorization", "Bearer " + context.token())
                .post(path.path());
        return processResponse(response, responseType);


    }

    public static <I, R> ApiResponse<R> doPostAnonymous(Path path, I input, Class<R> responseType) {
        var response = givenThis(input)
                .post(path.path());
        return processResponse(response, responseType);
    }

    public static <I, R> ApiResponse<R> doGet(ApiContext context, Path path, Class<R> responseType, Object... params) {
        var response = givenThis()
                .header("Authorization", "Bearer " + context.token())
                .get(path.path(), params)
                .then();
        return processResponse(response.extract().response(), responseType);
    }

    private static RequestSpecification givenThis() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .when();
    }

    private static RequestSpecification givenThis(Object postBody) {
        return givenThis().body(postBody);
    }

    private static <R> ApiResponse<R> processResponse(Response response, Class<R> responseType) {
        if (response.getStatusCode() == 200) {
            return new ApiResponse<>(response.as(responseType));
        } else {
            return new ApiResponse<>(response.as(ErrorResponse.class));
        }
    }

}
