package dev.leocamacho.demo.tests.api.paths;

import dev.leocamacho.demo.api.types.ErrorResponse;

public class ApiResponse<R> {
    private R response;
    private ErrorResponse error;

    public ApiResponse(R response) {
        this.response = response;
    }

    protected ApiResponse(ErrorResponse error) {
        this.error = error;
    }

    public R response() {
        if (error != null) {
            throw new RuntimeException(error.message());
        }
        return response;
    }

    public ErrorResponse error() {
        return error;
    }
}
