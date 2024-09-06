package dev.leocamacho.demo.handlers.commands;

public interface EncodePasswordHandler {
    record Command(String password){}
    sealed interface Result {
        record Success(String encodedPassword) implements Result {
        }
    }

    Result handle(Command command);
}
