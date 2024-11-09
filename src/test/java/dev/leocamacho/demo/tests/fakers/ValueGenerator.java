package dev.leocamacho.demo.tests.fakers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValueGenerator {

    private static final Random RANDOM = new Random();
    private static final Map<String, Object> valueMap = new HashMap<>();

    /**
     * Generates a value based on the input string.
     * <p>
     * The input string can be one of the following:
     * <ul>
     *     <li>String: The input string must be in the format {String:4}, where 4 is the length of the string to be generated.</li>
     *     <li>Long: The input string must be in the format {Long:<4>10}, where <4 is the minimum value and >10 is the maximum value of the long to be generated.</li>
     *     <li>Date: The input string must be in the format {Date:[+-]10d}, where [+-] is the sign of the days to be added or subtracted from the current date and <days> is the number of days to be added or subtracted.</li>
     *     <li>Integer: The input string must be a number.</li>
     *     <li>Long: The input string must be a number followed by the letter 'L'.</li>
     *     <li>LocalDate: The input string must be in the format yyyy-MM-dd.</li>
     *     <li>LocalTime: The input string must be in the format HH:mm:ss.</li>
     *     <li>LocalDateTime: The input string must be in the format yyyy-MM-ddTHH:mm:ss.</li>
     *     <li>String: Any other string.</li>
     *     <li>Reference: The input string must be in the format {$user}, where user is the reference user.</li>
     * </ul>
     *
     * @param input The input string.
     * @return The generated value.
     */
    public static Object generateValue(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        if (input.matches("\\{\\$\\w+}")) {
            return getValueFromMap(input);
        }
        if (input.contains("{") && input.contains("}")) {
            if (input.matches(".*\\{String:\\d+(:\\w+)?}.*")) {
                return generateRandomStringWith(input);
            } else if (input.matches(".*\\{String:\\d+(:\\w+)?(:\\$\\w+)?}.*")) {
                return generateRandomStringWithSuffix(input);
            } else if (input.matches("\\{Long:<\\d+>\\d+}")) {
                return generateRandomLong(input);
            } else if (input.matches("\\{Date:[+-]\\d+d}")) {
                return generateRandomDate(input);
            } else if (input.matches("\\d+")) {
                return Integer.parseInt(input);
            } else if (input.matches("\\d+L")) {
                return Long.parseLong(input.substring(0, input.length() - 1));
            } else if (input.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
            } else if (input.matches("\\d{2}:\\d{2}:\\d{2}")) {
                return LocalTime.parse(input, DateTimeFormatter.ISO_LOCAL_TIME);
            } else if (input.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                return LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                return input;
            }
        } else {
            return input;
        }
    }

    private static String generateRandomStringWith(String input) {
        Matcher matcher = Pattern.compile("\\{String:(\\d+)(?::(\\w+))?}").matcher(input);
        if (matcher.find()) {
            int length = Integer.parseInt(matcher.group(1));
            String randomString = RANDOM.ints('a', 'z' + 1)
                    .limit(length)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            if (matcher.group(2) != null) {
                valueMap.put(matcher.group(2), randomString);
            }
            return matcher.replaceFirst(randomString);
        }
        throw new IllegalArgumentException("Invalid String pattern");
    }
    private static String generateRandomStringWithSuffix(String input) {
    Matcher matcher = Pattern.compile("\\{String:(\\d+)(:\\w+)?\\:\\$(\\w+)}").matcher(input);
        if (matcher.find()) {
            int length = Integer.parseInt(matcher.group(1));
            String randomString = RANDOM.ints('a', 'z' + 1)
                    .limit(length)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            if (matcher.group(3) != null) {
                valueMap.put(matcher.group(3), randomString);
            }
            return matcher.replaceFirst(randomString);
        }
        throw new IllegalArgumentException("Invalid String pattern");
    }

    private static Long generateRandomLong(String input) {
        Matcher matcher = Pattern.compile("\\{Long:<(\\d+)>(\\d+)}").matcher(input);
        if (matcher.matches()) {
            long min = Long.parseLong(matcher.group(1));
            long max = Long.parseLong(matcher.group(2));
            return min + (long) (RANDOM.nextDouble() * (max - min));
        }
        throw new IllegalArgumentException("Invalid Long pattern");
    }

    private static LocalDate generateRandomDate(String input) {
        Matcher matcher = Pattern.compile("\\{Date:([+-])(\\d+)d}").matcher(input);
        if (matcher.matches()) {
            int days = Integer.parseInt(matcher.group(2));
            return matcher.group(1).equals("+") ? LocalDate.now().plusDays(days) : LocalDate.now().minusDays(days);
        }
        throw new IllegalArgumentException("Invalid Date pattern");
    }

    private static Object getValueFromMap(String input) {
        String key = input.substring(2, input.length() - 1);
        if (valueMap.containsKey(key)) {
            return valueMap.get(key);
        }
        throw new IllegalArgumentException("No value found for key: " + key);
    }
}