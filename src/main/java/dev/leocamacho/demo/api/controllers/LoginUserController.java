package dev.leocamacho.demo.api.controllers;

import dev.leocamacho.demo.api.request.LoginUserRequest;
import dev.leocamacho.demo.api.responses.ErrorResponse;
import dev.leocamacho.demo.api.responses.LoginResponse;
import dev.leocamacho.demo.exception.ErrorCode;
import dev.leocamacho.demo.handlers.commands.LoginUserHandler;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class LoginUserController {

    @Autowired
    private LoginUserHandler handler;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginUserRequest request) {
        var result = handler.handle(new LoginUserHandler.Command(
                request.username(),
                request.password()
        ));
        return switch (result) {
            case LoginUserHandler.Result.Success success ->
                    ResponseEntity.ok(new LoginResponse(success.token(), success.name(), success.email()));
            case LoginUserHandler.Result.InvalidCredentials ignored ->
                    ResponseEntity.badRequest().body(ErrorResponse.of(
                            ErrorCode.INVALID_CREDENTIALS, SessionContextHolder.getSession().correlationId()));
        };
    }
}
