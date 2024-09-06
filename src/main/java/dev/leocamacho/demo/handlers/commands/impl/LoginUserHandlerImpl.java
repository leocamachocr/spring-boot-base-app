package dev.leocamacho.demo.handlers.commands.impl;

import dev.leocamacho.demo.handlers.commands.LoginUserHandler;
import dev.leocamacho.demo.models.AuthenticatedUser;
import dev.leocamacho.demo.security.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class LoginUserHandlerImpl implements LoginUserHandler {
    @Autowired

    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtProvider jwtProvider;

    @Override
    public Result handle(Command command) {
        Authentication authentication = authenticate(command);
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return new Result.Success(jwtProvider.generateToken(user));
    }

    private Authentication authenticate(Command command) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(command.email(), command.password())
        );
    }
}
