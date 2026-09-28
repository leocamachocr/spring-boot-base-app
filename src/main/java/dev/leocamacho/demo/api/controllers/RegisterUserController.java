package dev.leocamacho.demo.api.controllers;

import dev.leocamacho.demo.api.request.RegisterUserRequest;
import dev.leocamacho.demo.api.responses.ErrorResponse;
import dev.leocamacho.demo.api.responses.Response;
import dev.leocamacho.demo.exception.ErrorCode;
import dev.leocamacho.demo.handlers.commands.RegisterUserHandler;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class RegisterUserController {

    @Autowired
    private RegisterUserHandler handler;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterUserRequest request) {
        var result = handler.handle(new RegisterUserHandler.Command(
                request.user(),
                request.email(),
                request.password()
        ));
        var correlationId = SessionContextHolder.getSession().correlationId();
        return switch (result) {
            case RegisterUserHandler.Result.Success success ->
                    ResponseEntity.ok(new Response(success.id().toString()));
            case RegisterUserHandler.Result.InvalidFields invalidFields ->
                    ResponseEntity.badRequest().body(
                            ErrorResponse.of(ErrorCode.REQUIRED_FIELDS, correlationId, invalidFields.fields()));
            case RegisterUserHandler.Result.EmailAlreadyExists ignored ->
                    ResponseEntity.badRequest().body(
                            ErrorResponse.of(ErrorCode.EMAIL_ALREADY_EXISTS, correlationId));
        };
    }
}
