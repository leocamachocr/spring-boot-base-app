package dev.leocamacho.demo.handlers.commands;

import dev.leocamacho.demo.jpa.entities.UserEntity;

public interface RegisterUserHandler {

    Result handle(Command command);

    sealed interface Result {
        record Success(UserEntity user) implements Result {
        }

        record InvalidFields(String... fields) implements Result {
        }
        record EmailAlreadyExists() implements Result {
        }

    }

    record Command(String name, String email, String password) { }
}
