package dev.leocamacho.demo.api.controllers;

import dev.leocamacho.demo.api.responses.ErrorResponse;
import dev.leocamacho.demo.api.responses.UserResponse;
import dev.leocamacho.demo.exception.ErrorCode;
import dev.leocamacho.demo.handlers.queries.GetCurrentUserQuery;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/private/users")
public class UserQueriesController {

    @Autowired
    private GetCurrentUserQuery getCurrentUserQuery;

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        var session = SessionContextHolder.getSession();
        return switch (getCurrentUserQuery.query(session.email())) {
            case GetCurrentUserQuery.Result.Success success ->
                    ResponseEntity.ok(new UserResponse(success.name(), success.email()));
            case GetCurrentUserQuery.Result.UserNotFound ignored ->
                    ResponseEntity.badRequest().body(
                            ErrorResponse.of(ErrorCode.INVALID_USER, session.correlationId()));
        };
    }
}
