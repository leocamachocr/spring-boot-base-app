package dev.leocamacho.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.function.Function;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        for (Integer number : numbers) {
            Function<Integer, Integer> result = integer -> integer + 2;
            System.out.println(result.apply(number));
        }

        numbers.stream()
                .map(integer -> integer + 2)
                .forEach(System.out::println);
        SpringApplication.run(App.class, args);
    }
}
