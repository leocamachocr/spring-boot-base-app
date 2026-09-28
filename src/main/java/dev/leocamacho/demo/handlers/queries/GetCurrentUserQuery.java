package dev.leocamacho.demo.handlers.queries;

import java.util.UUID;

public interface GetCurrentUserQuery {

    Result query(String email);

    sealed interface Result {
        record Success(UUID id, String name, String email) implements Result {
        }

        record UserNotFound() implements Result {
        }

    }
}
