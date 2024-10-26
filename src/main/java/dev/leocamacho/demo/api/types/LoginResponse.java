package dev.leocamacho.demo.api.types;

public record LoginResponse(
        String token,
        String email,
        String name) {

}
