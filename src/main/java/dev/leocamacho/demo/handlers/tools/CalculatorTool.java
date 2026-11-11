package dev.leocamacho.demo.handlers.tools;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Arithmetic for the chatbot assistant. Invalid input produces an error result for the assistant, never an
 * exception.
 */
@Component
public class CalculatorTool implements ChatTool {

    private static final String NAME = "calculate";
    private static final String OPERATION = "operation";
    private static final String A = "a";
    private static final String B = "b";
    private static final String RESULT_KEY = "result";
    private static final String ERROR_KEY = "error";
    private static final List<String> OPERATIONS = List.of("add", "subtract", "multiply", "divide", "sqrt");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Calculator for arithmetic. Always call it for any addition, subtraction, multiplication, division "
                + "or square root instead of computing the result yourself. For sqrt only 'a' is used.";
    }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        OPERATION, Map.of("type", "string", "enum", OPERATIONS,
                                "description", "The operation to perform"),
                        A, Map.of("type", "number", "description", "First operand (the radicand for sqrt)"),
                        B, Map.of("type", "number", "description", "Second operand; not used by sqrt")),
                "required", List.of(OPERATION, A));
    }

    @Override
    public Map<String, Object> execute(UUID userId, Map<String, Object> arguments) {
        var operation = arguments.get(OPERATION) instanceof String value ? value.toLowerCase(Locale.ROOT) : "";
        var a = number(arguments, A);
        if (a.isEmpty()) {
            return error("Argument 'a' must be a number");
        }
        if ("sqrt".equals(operation)) {
            return a.get() < 0 ? error("Square root of a negative number") : result(Math.sqrt(a.get()));
        }
        if (!OPERATIONS.contains(operation)) {
            return error("Unknown operation: " + operation);
        }
        var b = number(arguments, B);
        if (b.isEmpty()) {
            return error("Argument 'b' must be a number for " + operation);
        }
        return switch (operation) {
            case "add" -> result(a.get() + b.get());
            case "subtract" -> result(a.get() - b.get());
            case "multiply" -> result(a.get() * b.get());
            case "divide" -> b.get() == 0 ? error("Division by zero") : result(a.get() / b.get());
            default -> error("Unknown operation: " + operation);
        };
    }

    private static Optional<Double> number(Map<String, Object> arguments, String key) {
        return arguments.get(key) instanceof Number value ? Optional.of(value.doubleValue()) : Optional.empty();
    }

    private static Map<String, Object> result(double value) {
        return Map.of(RESULT_KEY, value);
    }

    private static Map<String, Object> error(String message) {
        return Map.of(ERROR_KEY, message);
    }
}
