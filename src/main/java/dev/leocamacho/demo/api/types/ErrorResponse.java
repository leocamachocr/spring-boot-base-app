package dev.leocamacho.demo.api.types;

import java.util.UUID;

public record ErrorResponse(
        String message,
        Integer code,
        UUID correlationId,
        String... params
) {

}
