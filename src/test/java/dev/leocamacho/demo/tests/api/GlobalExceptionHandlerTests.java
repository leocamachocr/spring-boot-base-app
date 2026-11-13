package dev.leocamacho.demo.tests.api;

import dev.leocamacho.demo.api.GlobalExceptionHandler;
import dev.leocamacho.demo.exception.ErrorCode;
import dev.leocamacho.demo.session.Session;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GlobalExceptionHandlerTests {

    private static final String CHAT_PATH = "/api/private/chat/messages";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    public void setUp() {
        SessionContextHolder.setSession(Session.newBuilder().buildAnonymous());
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void unknownResourceIsNotFound() {
        var response = handler.handleNotFound(new NoResourceFoundException(HttpMethod.POST, CHAT_PATH, CHAT_PATH));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.code(), response.getBody().code());
    }

    @Test
    public void unknownHandlerIsNotFound() {
        var response = handler.handleNotFound(new NoHandlerFoundException("POST", CHAT_PATH, new HttpHeaders()));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.code(), response.getBody().code());
    }
}
