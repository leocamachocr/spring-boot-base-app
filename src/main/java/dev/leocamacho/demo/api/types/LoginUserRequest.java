package dev.leocamacho.demo.api.types;

public record LoginUserRequest(
        String email,
        String password) {
}
