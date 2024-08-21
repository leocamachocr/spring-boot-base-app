package dev.leocamacho.demo.api.types;

public record RegisterUserRequest(
        String user,
        String email,
        String password
) {
}
