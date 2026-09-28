package dev.leocamacho.demo.api.request;

public record LoginUserRequest(
        String username,
        String password) {
}
