package dev.leocamacho.demo.api.rests;

import dev.leocamacho.demo.api.types.RegisterUserRequest;
import dev.leocamacho.demo.api.types.Response;
import dev.leocamacho.demo.handlers.commands.RegisterUserHandler;
import dev.leocamacho.demo.models.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class RegisterUserController {

    @Autowired
    private RegisterUserHandler handler;

    @PostMapping("/register")
    public Response register(@RequestBody RegisterUserRequest request) {
        var result = handler.handle(new RegisterUserHandler.Command(
                request.user(),
                request.email(),
                request.password()
        ));
        return switch (result) {
            case RegisterUserHandler.Result.Success success -> new Response(success.user().getId().toString());
            case RegisterUserHandler.Result.InvalidFields invalidFields ->
                    throw BaseException.exceptionBuilder().params(List.of(invalidFields.fields())).build();
        };


    }
}
