package dev.leocamacho.demo.tests.handlers.tools;

import dev.leocamacho.demo.handlers.tools.CalculatorTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalculatorToolTests {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final double TEN = 10.0;
    private static final double FOUR = 4.0;
    private static final double SIXTEEN = 16.0;
    private static final long TWELVE = 12L;

    private final CalculatorTool calculator = new CalculatorTool();

    @ParameterizedTest
    @MethodSource("validOperations")
    public void validOperationsReturnTheirResult(String operation, Object a, Object b, double expected) {
        var result = calculator.execute(USER_ID, arguments(operation, a, b));

        assertEquals(Map.of("result", expected), result);
    }

    @ParameterizedTest
    @MethodSource("invalidOperations")
    public void invalidOperationsReturnAnErrorInsteadOfFailing(String operation, Object a, Object b, String error) {
        var result = calculator.execute(USER_ID, arguments(operation, a, b));

        assertEquals(Map.of("error", error), result);
    }

    @Test
    public void schemaDeclaresTheOperationsAndRequiredArguments() {
        var parameters = calculator.parameters();

        assertEquals("calculate", calculator.name());
        assertEquals("object", parameters.get("type"));
        assertTrue(parameters.toString().contains("sqrt"));
        assertEquals(List.of("operation", "a"), parameters.get("required"));
    }

    private static Stream<Arguments> validOperations() {
        return Stream.of(
                Arguments.of("add", TEN, FOUR, TEN + FOUR),
                Arguments.of("subtract", TEN, FOUR, TEN - FOUR),
                Arguments.of("multiply", TEN, FOUR, TEN * FOUR),
                Arguments.of("divide", TEN, FOUR, TEN / FOUR),
                Arguments.of("sqrt", SIXTEEN, null, FOUR),
                Arguments.of("ADD", 2, TWELVE, 2.0 + TWELVE));
    }

    private static Stream<Arguments> invalidOperations() {
        return Stream.of(
                Arguments.of("divide", TEN, 0, "Division by zero"),
                Arguments.of("sqrt", -FOUR, null, "Square root of a negative number"),
                Arguments.of("add", TEN, null, "Argument 'b' must be a number for add"),
                Arguments.of("add", "ten", FOUR, "Argument 'a' must be a number"),
                Arguments.of("power", TEN, FOUR, "Unknown operation: power"));
    }

    private static Map<String, Object> arguments(String operation, Object a, Object b) {
        var arguments = new HashMap<String, Object>();
        arguments.put("operation", operation);
        arguments.put("a", a);
        if (b != null) {
            arguments.put("b", b);
        }
        return arguments;
    }
}
