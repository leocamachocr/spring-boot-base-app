package dev.leocamacho.demo.handlers.commands;

import java.util.UUID;

public interface RegisterUserHandler {

    Result handle(Command command);

    sealed interface Result {
        record Success(UUID id) implements Result {
        }

        record InvalidFields(String... fields) implements Result {
        }

        record EmailAlreadyExists() implements Result {
        }

    }

    record Command(String name, String email, String password) { }
}
