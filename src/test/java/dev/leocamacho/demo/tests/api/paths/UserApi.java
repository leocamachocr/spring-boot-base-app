package dev.leocamacho.demo.tests.api.paths;

import dev.leocamacho.demo.api.types.*;

public class UserApi {

    private ApiContext context;

    public UserApi(ApiContext context) {
        this.context = context;
    }

    public ApiResponse<Response> registerUser(RegisterUserRequest request) {
        return ApiVerbs.doPostAnonymous(
                Path.Public.User.register,
                request,
                Response.class
        );
    }

    public ApiResponse<LoginResponse> loginUser(LoginUserRequest request) {
        return ApiVerbs.doPostAnonymous(
                Path.Public.User.login,
                request,
                LoginResponse.class
        );


    }

    public ApiResponse<UserResponse> loggedUser() {
        return ApiVerbs.doGet(
                context,
                Path.Private.User.loggedUser,
                UserResponse.class
        );

    }
}
