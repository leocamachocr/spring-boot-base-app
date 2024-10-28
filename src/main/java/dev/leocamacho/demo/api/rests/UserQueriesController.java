package dev.leocamacho.demo.api.rests;

import dev.leocamacho.demo.api.types.UserResponse;
import dev.leocamacho.demo.handlers.queries.GetCurrentUserQuery;
import dev.leocamacho.demo.models.BaseException;
import dev.leocamacho.demo.models.ErrorCode;
import dev.leocamacho.demo.session.SessionContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/private/users")
public class UserQueriesController {

    @Autowired
    private GetCurrentUserQuery getCurrentUserQuery;

    @GetMapping("/current")
    public UserResponse getCurrentUser() {

        var result = getCurrentUserQuery.query(SessionContextHolder.getSession().email());
        switch (result) {
            case GetCurrentUserQuery.Result.Success success -> {
                return new UserResponse(success.user().getName(), success.user().getEmail());
            }
            case GetCurrentUserQuery.Result.UserNotFound ignored -> {
                throw BaseException.exceptionBuilder()
                        .code(ErrorCode.INVALID_USER)
                        .build();
            }
        }
    }
}
