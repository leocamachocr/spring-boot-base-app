package dev.leocamacho.demo.handlers.queries;

import dev.leocamacho.demo.jpa.entities.UserEntity;

public interface GetCurrentUserQuery {

    Result query(String email);

    sealed interface Result {
        record Success(UserEntity user) implements Result {
        }

        final class UserNotFound implements Result {
        }

    }
}
