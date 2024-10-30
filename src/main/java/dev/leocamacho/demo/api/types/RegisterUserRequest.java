package dev.leocamacho.demo.api.types;

public record RegisterUserRequest(
        String name,
        String email,
        String password
) {
}
