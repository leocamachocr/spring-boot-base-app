package dev.leocamacho.demo.api.request;

public record RegisterUserRequest(
        String user,
        String email,
        String password
) {
}
