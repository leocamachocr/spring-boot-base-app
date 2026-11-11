package dev.leocamacho.demo.exception;


public enum ErrorCode {

    // Basic Business Validations 1000 - 1999
    ERROR_NOT_IDENTIFIED(1001, "Error not identified"),
    REQUIRED_FIELDS(1002, "Required fields are missing"),
    UNKNOWN_ERROR(1003, "Unknown error"),
    RESOURCE_NOT_FOUND(1004, "Resource not found"),

    // Security Validations
    UNAUTHORIZED(401, "Unauthorized"),
    INVALID_USER(2001, "Invalid User"),
    EMAIL_ALREADY_EXISTS(2002, "Email already exists"),
    INVALID_CREDENTIALS(2003, "Invalid credentials"),

    // Chat 3000 - 3999
    CHAT_INVALID_MESSAGE(3001, "Invalid chat message"),
    CHAT_CONVERSATION_NOT_FOUND(3002, "Conversation not found"),
    CHAT_ASSISTANT_UNAVAILABLE(3003, "The assistant is unavailable"),
    CHAT_NO_ANSWER(3004, "The assistant could not answer");

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
