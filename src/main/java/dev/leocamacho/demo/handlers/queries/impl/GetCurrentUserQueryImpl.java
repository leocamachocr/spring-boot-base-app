package dev.leocamacho.demo.handlers.queries.impl;

import dev.leocamacho.demo.handlers.queries.GetCurrentUserQuery;
import dev.leocamacho.demo.persistence.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetCurrentUserQueryImpl implements GetCurrentUserQuery {
    @Autowired
    private UserRepository repository;

    @Override
    public Result query(String email) {
        if (email == null) {
            return new Result.UserNotFound();
        }
        return repository.findByEmail(email)
                .map(user -> (Result) new Result.Success(user.getId(), user.getName(), user.getEmail()))
                .orElseGet(Result.UserNotFound::new);
    }
}
