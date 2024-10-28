package dev.leocamacho.demo.models;


public enum ErrorCode {

    // Basic Business Validations 1000 - 1999
    ERROR_NOT_IDENTIFIED(1001, "Error not identified"),
    REQUIRED_FIELDS(1002, "Required fields are missing"),
    UNKNOWN_ERROR(1003, "Unknown error"),

    // Security Validations
    UNAUTHORIZED(401, "Unauthorized"),
    INVALID_USER(2001, "Invalid User"),
    EMAIL_ALREADY_EXISTS(2002, "Email already exists");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}