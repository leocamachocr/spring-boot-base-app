package dev.leocamacho.demo.api.rests;

import dev.leocamacho.demo.api.types.LoginUserRequest;
import dev.leocamacho.demo.api.types.Response;
import dev.leocamacho.demo.handlers.commands.LoginUserHandler;
import dev.leocamacho.demo.models.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Response login(@RequestBody LoginUserRequest request) {
        var result = handler.handle(new LoginUserHandler.Command(
                request.email(),
                request.password()
        ));
        return switch (result) {
            case LoginUserHandler.Result.Success success -> new Response(success.token());
            case LoginUserHandler.Result.InvalidCredentials invalidCredentials ->
                    throw BaseException.exceptionBuilder().build();

        };


    }
}
