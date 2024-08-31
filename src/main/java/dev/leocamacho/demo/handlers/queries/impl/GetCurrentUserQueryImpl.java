package dev.leocamacho.demo.handlers.queries.impl;

import dev.leocamacho.demo.handlers.queries.GetCurrentUserQuery;
import dev.leocamacho.demo.jpa.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetCurrentUserQueryImpl implements GetCurrentUserQuery {
    @Autowired
    private UserRepository repository;

    @Override
    public Result query(String email) {
        var user = repository.findByEmail(email);

        return user
                .map((it) -> (Result) new Result.Success(it))
                .orElseGet(Result.UserNotFound::new);
    }
}
