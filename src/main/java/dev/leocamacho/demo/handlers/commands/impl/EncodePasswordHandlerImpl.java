package dev.leocamacho.demo.handlers.commands.impl;

import dev.leocamacho.demo.handlers.commands.EncodePasswordHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class EncodePasswordHandlerImpl implements EncodePasswordHandler {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Result handle(Command command) {
        return new Result.Success(passwordEncoder.encode(command.password()));
    }
}
