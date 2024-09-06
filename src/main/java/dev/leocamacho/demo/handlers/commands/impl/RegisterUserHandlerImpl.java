package dev.leocamacho.demo.handlers.commands.impl;

import dev.leocamacho.demo.handlers.commands.EncodePasswordHandler;
import dev.leocamacho.demo.handlers.commands.RegisterUserHandler;
import dev.leocamacho.demo.jpa.entities.UserEntity;
import dev.leocamacho.demo.jpa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RegisterUserHandlerImpl implements RegisterUserHandler {
    @Autowired
    private UserRepository repository;
    @Autowired
    private EncodePasswordHandler encodePasswordHandler;

    public Result handle(Command command) {
        var check = checkInvalidFields(command);
        if (check != null) {
            return check;
        }
        var user = repository.save(toEntity(command));
        return new Result.Success(user);
    }

    private UserEntity toEntity(Command command) {
        return UserEntity.UserEntityBuilder.anUserEntity()
                .withName(command.name())
                .withEmail(command.email())
                .withPassword(encodePassword(command))
                .build();
    }

    private String encodePassword(Command command) {
        var result = encodePasswordHandler.handle(new EncodePasswordHandler.Command(command.password()));
        return ((EncodePasswordHandler.Result.Success) result).encodedPassword();
    }

    private Result.InvalidFields checkInvalidFields(Command command) {
        List<String> invalidFields = new ArrayList<>();

        if (command.name() == null || command.name().isBlank()) {
            invalidFields.add("name");
        }
        if (command.email() == null || command.email().isBlank()) {
            invalidFields.add("email");
        }
        if (command.password() == null || command.password().isBlank()) {
            invalidFields.add("password");
        }

        if (!invalidFields.isEmpty()) {
            return new Result.InvalidFields(invalidFields.toArray(new String[0]));
        } else return null;


    }
}
