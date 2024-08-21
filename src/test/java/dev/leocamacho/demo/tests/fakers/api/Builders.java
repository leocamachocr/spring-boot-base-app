package dev.leocamacho.demo.tests.fakers.api;

import dev.leocamacho.demo.api.types.RegisterUserRequest;

public class Builders {


    public static RegisterUserRequest buildRegisterUserRequestWithValidParams() {
        return buildRegisterUserRequestWithValidParams("user", "user@mail.com", "password");
    }

    public static RegisterUserRequest buildRegisterUserRequestWithValidParams(String user) {
        return buildRegisterUserRequestWithValidParams(user, user + "@mail.com", "password");
    }

    //create overloads for the method to generate different objects
    public static RegisterUserRequest buildRegisterUserRequestWithValidParams(
            String user,
            String email,
            String password
    ) {
        return new RegisterUserRequest(user, email, password);
    }

}
