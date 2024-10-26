package dev.leocamacho.demo.handlers.commands;

public interface LoginUserHandler {
    record Command(String email, String password) {}

    sealed interface Result {
        record Success(String token,String name,String email) implements Result {
        }

        record InvalidCredentials() implements Result {
        }
    }

    Result handle(Command command);
}
