package dev.leocamacho.demo.tests.api.paths;

import dev.leocamacho.demo.api.responses.LoginResponse;
import dev.leocamacho.demo.api.request.LoginUserRequest;
import dev.leocamacho.demo.api.request.RegisterUserRequest;
import dev.leocamacho.demo.api.responses.Response;
import dev.leocamacho.demo.api.responses.UserResponse;

public class UserApi {

    private ApiContext context;

    public UserApi(ApiContext context) {
        this.context = context;
    }

    public ApiResponse<Response> registerUser(RegisterUserRequest request) {
        return ApiVerbs.doPostAnonymous(
                Path.Public.User.REGISTER,
                request,
                Response.class
        );
    }

    public ApiResponse<LoginResponse> loginUser(LoginUserRequest request) {
        return ApiVerbs.doPostAnonymous(
                Path.Public.User.LOGIN,
                request,
                LoginResponse.class
        );


    }

    public ApiResponse<UserResponse> loggedUser() {
        return ApiVerbs.doGet(
                context,
                Path.Private.User.LOGGED_USER,
                UserResponse.class
        );

    }
}
