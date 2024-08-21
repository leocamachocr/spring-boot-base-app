package dev.leocamacho.demo.tests.structure;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static java.lang.String.format;

public class ArchUnitFacadeConstrains {

    @SafeVarargs
    public static ArchCondition<JavaClass> haveSpecifiedAnnotations(Class<? extends Annotation>... annotations) {
        String names = Arrays.stream(annotations).map(Class::getName)
                .collect(Collectors.joining(", "));
        return buildCondition(format("it must have annotations %s", names),
                (item, events) -> {
                    if (!mustHave(item, annotations)) {
                        events.add(violated(
                                item,
                                format("Class %s does not have %s", item.getName(), names)
                        ));
                    }
                });
    }

    @SafeVarargs
    public static ArchCondition<JavaClass> haveAnyAnnotations(Class<? extends Annotation>... annotations) {
        String names = Arrays.stream(annotations).map(Class::getName)
                .collect(Collectors.joining(", "));
        return buildCondition(format("it must have annotations %s", names),
                (item, events) -> {
                    if (!haveAny(item, annotations)) {
                        events.add(violated(
                                item,
                                format("Class %s does not have %s", item.getName(), names)
                        ));
                    }
                });
    }

    public static ArchCondition<JavaClass> haveMaxPublicMethods(int count) {
        return buildCondition(format("Must have a maximum of %d public methods", count),
                (item, events) -> {
                    long publicMethodCount = item.getMethods().stream()
                            .filter(ArchUnitFacadeConstrains::isPublic)
                            .count();
                    if (publicMethodCount > count) {
                        events.add(violated(
                                item,
                                format("Class %s has more than %d public methods", item.getName(), count)
                        ));
                    }
                });
    }

    public static ArchCondition<JavaClass> haveSpecifiedReturnTypes(String... allowedReturnTypes) {
        return buildCondition(format("Must have return types of public methods in %s", Arrays.toString(allowedReturnTypes)),
                (item, events) -> {
                    for (JavaMethod method : item.getMethods()) {
                        if (isPublic(method) && Arrays.stream(allowedReturnTypes).noneMatch(it ->
                                method.getReturnType().getName().contains(it)
                        )) {
                            events.add(violated(
                                    item,
                                    format(
                                            "Method %s in class %s has a return type not in %s",
                                            method.getName(),
                                            item.getName(),
                                            Arrays.toString(allowedReturnTypes)
                                    )
                            ));
                        }
                    }
                });
    }

    public static ArchCondition<JavaClass> haveSpecifiedParameterTypes(String... packageParameters) {
        return buildCondition(format("Must have parameter types of public methods in %s", Arrays.toString(packageParameters)),
                (item, events) -> {
                    item.getMethods().stream().filter(ArchUnitFacadeConstrains::isPublic).forEach(method -> {
                        method.getParameterTypes().forEach(param -> {
                            if (Arrays.stream(packageParameters).noneMatch(it -> param.getName().contains(it))) {
                                events.add(violated(
                                        item,
                                        format(
                                                "Method %s in class %s has a parameter type not in %s",
                                                method.getName(),
                                                item.getName(),
                                                Arrays.toString(packageParameters)
                                        )
                                ));
                            }
                        });
                    });
                });
    }

    @SafeVarargs
    public static ArchCondition<JavaClass> haveSpecifiedMethodAnnotations(Class<? extends Annotation>... annotations) {
        return buildCondition(format("Must have any of this annotation  in all public methods %s", Arrays.toString(annotations)),
                (item, events) -> item.getMethods().forEach(method -> {
                    if (isPublic(method) && containsAtLeast(method, annotations)) {
                        events.add(violated(
                                item,
                                format(
                                        "Public method %s in class %s does not have the required annotations",
                                        method.getName(),
                                        item.getName()
                                )
                        ));
                    }
                })
        );
    }

    public static ArchCondition<JavaClass> haveImplementedFrom(String parent) {
        return buildCondition(format("Must inherit from %s", parent),
                (item, events) -> {
                    if (item.getAllRawInterfaces().stream().noneMatch(it -> it.getName().contains(parent))) {
                        events.add(violated(
                                item,
                                format("Class %s does not inherit from %s", item.getName(), parent)
                        ));
                    }
                });
    }

    @SafeVarargs
    public static boolean containsAtLeast(JavaMethod method, Class<? extends Annotation>... annotations) {
        return Arrays.stream(annotations).anyMatch(method::isAnnotatedWith);
    }


    public static <T> ArchCondition<T> buildCondition(
            String description,
            ArchConditionPredicate<T> predicate
    ) {

        return new ArchCondition<>(description) {
            @Override
            public void check(T item, ConditionEvents events) {
                predicate.apply(item, events);
            }
        };
    }

    @SafeVarargs
    public static boolean mustHave(JavaClass item, Class<? extends Annotation>... annotation) {
        return Arrays.stream(annotation).allMatch(item::isAnnotatedWith);
    }

    @SafeVarargs
    public static boolean haveAny(JavaClass item, Class<? extends Annotation>... annotation) {
        return Arrays.stream(annotation).anyMatch(item::isAnnotatedWith);
    }

    public static boolean isPublic(JavaMethod method) {
        return method.getModifiers().stream().anyMatch(modifier -> modifier.name().equals("PUBLIC"));
    }

    public static interface ArchConditionPredicate<T> {
        void apply(T item, ConditionEvents events);
    }
}
