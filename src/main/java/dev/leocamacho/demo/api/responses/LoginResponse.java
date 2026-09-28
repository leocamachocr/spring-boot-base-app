package dev.leocamacho.demo.api.responses;

public record LoginResponse(
        String token,
        String name,
        String email
        ) {

}
