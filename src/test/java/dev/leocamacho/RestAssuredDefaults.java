package dev.leocamacho;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public class RestAssuredDefaults {


    private static final String DEFAULT_HOST = System.getProperty("host", "http://localhost");
    private static final int DEFAULT_PORT = Integer.parseInt(System.getProperty("port", "8080"));


    public static void setupTargetHost(String host, int port) {
        RestAssured.baseURI = host;
        RestAssured.port = port;
    }

    public static RequestSpecification givenThis(Object postBody) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(postBody)
                .when();
    }

}
